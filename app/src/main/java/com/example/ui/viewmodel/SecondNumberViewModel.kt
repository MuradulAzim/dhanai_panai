package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CallDirection
import com.example.data.local.entity.CallEntity
import com.example.data.local.entity.CallStatus
import com.example.data.local.entity.ConversationSummary
import com.example.data.local.entity.MessageDirection
import com.example.data.local.entity.MessageEntity
import com.example.data.repository.SecondNumberRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppNavTab {
    DIALER,
    CALLS,
    MESSAGES,
    SETTINGS
}

data class ActiveCallUiState(
    val callId: Long,
    val callSid: String?,
    val phoneNumber: String,
    val contactName: String? = null,
    val status: CallStatus = CallStatus.RINGING,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeaker: Boolean = false,
    val isIncoming: Boolean = false
)

class SecondNumberViewModel(application: Application) : AndroidViewModel(application) {
    val repository = SecondNumberRepository(application)

    val twilioPhoneNumber = repository.twilioPhoneNumber
    val isServerConnected = repository.isServerConnected
    val serverStatusMessage = repository.serverStatusMessage

    private val _currentTab = MutableStateFlow(AppNavTab.DIALER)
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    private val _dialedNumber = MutableStateFlow("")
    val dialedNumber: StateFlow<String> = _dialedNumber.asStateFlow()

    private val _activeCall = MutableStateFlow<ActiveCallUiState?>(null)
    val activeCall: StateFlow<ActiveCallUiState?> = _activeCall.asStateFlow()

    private val _selectedConversation = MutableStateFlow<String?>(null)
    val selectedConversation: StateFlow<String?> = _selectedConversation.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    // Recent calls
    val allCalls: StateFlow<List<CallEntity>> = repository.allCalls.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val missedCalls: StateFlow<List<CallEntity>> = repository.missedCalls.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Conversations mapped from all messages
    val conversations: StateFlow<List<ConversationSummary>> = repository.allMessages.map { messages ->
        messages.groupBy { it.conversationNumber }.map { (number, msgList) ->
            val sorted = msgList.sortedByDescending { it.timestamp }
            val latest = sorted.first()
            val unreadCount = msgList.count { !it.isRead && it.direction == MessageDirection.INCOMING }
            ConversationSummary(
                conversationNumber = number,
                contactName = null,
                lastMessageBody = latest.body,
                lastMessageTimestamp = latest.timestamp,
                lastMessageDirection = latest.direction,
                unreadCount = unreadCount
            )
        }.sortedByDescending { it.lastMessageTimestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalUnreadCount = repository.totalUnreadCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    private var callTimerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            repository.checkServerStatus()
        }
    }

    fun selectTab(tab: AppNavTab) {
        _currentTab.value = tab
    }

    // Dialer controls
    fun appendDigit(digit: String) {
        if (_dialedNumber.value.length < 24) {
            _dialedNumber.value += digit
        }
    }

    fun deleteDigit() {
        if (_dialedNumber.value.isNotEmpty()) {
            _dialedNumber.value = _dialedNumber.value.dropLast(1)
        }
    }

    fun clearDialer() {
        _dialedNumber.value = ""
    }

    fun setDialedNumber(number: String) {
        _dialedNumber.value = number
    }

    // Call Actions
    fun startCall(destinationNumber: String? = null) {
        val numberToCall = (destinationNumber ?: _dialedNumber.value).trim()
        if (numberToCall.isBlank()) {
            emitToast("Please enter a valid phone number")
            return
        }

        viewModelScope.launch {
            val result = repository.initiateOutboundCall(numberToCall)
            result.onSuccess { callEntity ->
                _activeCall.value = ActiveCallUiState(
                    callId = callEntity.id,
                    callSid = callEntity.callSid,
                    phoneNumber = numberToCall,
                    status = CallStatus.RINGING,
                    isIncoming = false
                )
                startCallSimulation(callEntity.id)
            }.onFailure {
                emitToast("Call failed: ${it.message}")
            }
        }
    }

    private fun startCallSimulation(callId: Long) {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            // Simulate ringing for 3 seconds then connect
            delay(3000)
            _activeCall.value = _activeCall.value?.copy(status = CallStatus.IN_PROGRESS)
            repository.updateCallStatus(callId, CallStatus.IN_PROGRESS)

            // Increment call duration each second
            var seconds = 0
            while (_activeCall.value?.status == CallStatus.IN_PROGRESS) {
                delay(1000)
                seconds++
                _activeCall.value = _activeCall.value?.copy(durationSeconds = seconds)
            }
        }
    }

    fun answerIncomingCall() {
        val current = _activeCall.value ?: return
        viewModelScope.launch {
            _activeCall.value = current.copy(status = CallStatus.IN_PROGRESS)
            repository.updateCallStatus(current.callId, CallStatus.IN_PROGRESS)

            callTimerJob?.cancel()
            callTimerJob = viewModelScope.launch {
                var seconds = 0
                while (_activeCall.value?.status == CallStatus.IN_PROGRESS) {
                    delay(1000)
                    seconds++
                    _activeCall.value = _activeCall.value?.copy(durationSeconds = seconds)
                }
            }
        }
    }

    fun rejectIncomingCall() {
        val current = _activeCall.value ?: return
        viewModelScope.launch {
            callTimerJob?.cancel()
            repository.terminateCall(current.callId, current.callSid, 0)
            _activeCall.value = null
        }
    }

    fun toggleMute() {
        _activeCall.value = _activeCall.value?.let { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleSpeaker() {
        _activeCall.value = _activeCall.value?.let { it.copy(isSpeaker = !it.isSpeaker) }
    }

    fun hangupCall() {
        val current = _activeCall.value ?: return
        callTimerJob?.cancel()
        val duration = current.durationSeconds
        viewModelScope.launch {
            _activeCall.value = current.copy(status = CallStatus.COMPLETED)
            repository.terminateCall(current.callId, current.callSid, duration)
            delay(1200) // Brief showing of "Call Ended"
            _activeCall.value = null
        }
    }

    // Message Actions
    fun openConversation(phoneNumber: String) {
        _selectedConversation.value = phoneNumber
        viewModelScope.launch {
            repository.markConversationRead(phoneNumber)
        }
    }

    fun closeConversation() {
        _selectedConversation.value = null
    }

    fun sendSms(text: String) {
        val conversationNumber = _selectedConversation.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            val result = repository.sendSms(conversationNumber, text.trim())
            result.onFailure {
                emitToast("Could not send SMS: ${it.message}")
            }
        }
    }

    // Settings & Simulations
    fun testServerConnection() {
        viewModelScope.launch {
            emitToast("Connecting to server...")
            val result = repository.checkServerStatus()
            result.onSuccess {
                emitToast("Server online! Twilio: ${if (it.twilioConfigured) "Configured (${it.twilioPhoneNumber})" else "Not configured"}")
            }.onFailure {
                emitToast("Connection failed: ${it.localizedMessage}")
            }
        }
    }

    fun saveServerSettings(url: String, token: String) {
        repository.saveServerConfig(url, token)
        emitToast("Settings saved")
        testServerConnection()
    }

    fun saveAndSyncTwilioCredentials(sid: String, token: String, phoneNumber: String) {
        viewModelScope.launch {
            if (sid.isBlank() || token.isBlank() || phoneNumber.isBlank()) {
                emitToast("Please fill in Account SID, Auth Token, and Phone Number")
                return@launch
            }
            emitToast("Syncing Twilio credentials...")
            val result = repository.syncTwilioCredentialsToBackend(sid, token, phoneNumber)
            result.onSuccess { msg ->
                emitToast(msg)
            }.onFailure { e ->
                emitToast("Saved locally. Backend sync notice: ${e.localizedMessage}")
            }
        }
    }

    fun simulateIncomingCall(fromNumber: String = "+1 (555) 382-9011") {
        viewModelScope.launch {
            val callId = repository.recordIncomingCall(fromNumber)
            _activeCall.value = ActiveCallUiState(
                callId = callId,
                callSid = "SIM-CALL-${System.currentTimeMillis()}",
                phoneNumber = fromNumber,
                status = CallStatus.RINGING,
                isIncoming = true
            )
            emitToast("Incoming call simulated from $fromNumber")
        }
    }

    fun simulateIncomingSms(fromNumber: String = "+1 (555) 382-9011", text: String = "Hey, are you free for a call on your second number?") {
        viewModelScope.launch {
            repository.receiveIncomingSms(fromNumber, text)
            emitToast("Incoming SMS simulated from $fromNumber")
        }
    }

    fun deleteCall(id: Long) {
        viewModelScope.launch {
            repository.deleteCall(id)
            emitToast("Call deleted")
        }
    }

    fun deleteConversation(phoneNumber: String) {
        viewModelScope.launch {
            repository.deleteConversation(phoneNumber)
            if (_selectedConversation.value == phoneNumber) {
                _selectedConversation.value = null
            }
            emitToast("Conversation deleted")
        }
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }
}
