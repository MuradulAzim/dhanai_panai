package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.billing.PlayBillingManager
import com.example.data.local.entity.CallDirection
import com.example.data.local.entity.CallEntity
import com.example.data.local.entity.CallStatus
import com.example.data.local.entity.ConversationSummary
import com.example.data.local.entity.MessageDirection
import com.example.data.local.entity.MessageEntity
import com.example.data.remote.models.AdminStatsDto
import com.example.data.remote.models.ApiUserDto
import com.example.data.remote.models.RatesAndBankingDto
import com.example.data.remote.models.RechargeRequestDto
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
    STORE,
    AI_CHAT,
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
            repository.fetchNumberCatalog()
            repository.fetchUserRates()
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

    // Reseller & Quota States
    val userApiKey = repository.userApiKey.asStateFlow()
    val smsBalance = repository.smsBalance.asStateFlow()
    val callMinutesBalance = repository.callMinutesBalance.asStateFlow()
    val ratesAndBanking = repository.ratesAndBanking.asStateFlow()
    val isAdmin = repository.isAdmin.asStateFlow()

    private val _adminRequests = MutableStateFlow<List<RechargeRequestDto>>(emptyList())
    val adminRequests: StateFlow<List<RechargeRequestDto>> = _adminRequests.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<ApiUserDto>>(emptyList())
    val adminUsers: StateFlow<List<ApiUserDto>> = _adminUsers.asStateFlow()

    private val _adminStats = MutableStateFlow<AdminStatsDto?>(null)
    val adminStats: StateFlow<AdminStatsDto?> = _adminStats.asStateFlow()

    fun fetchUserRates() {
        viewModelScope.launch {
            repository.fetchUserRates()
        }
    }

    fun submitPackageRequest(
        userName: String,
        userContact: String,
        smsRequested: Int,
        minutesRequested: Int,
        totalAmountBdt: Double,
        paymentMethod: String,
        senderNumber: String,
        transactionId: String,
        screenshotBase64: String? = null,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.submitPackageRequest(
                userName, userContact, smsRequested, minutesRequested,
                totalAmountBdt, paymentMethod, senderNumber, transactionId, screenshotBase64
            )
            result.onSuccess { reqId ->
                emitToast("পেমেন্ট রিকোয়েস্ট সাবমিট হয়েছে (ID: $reqId)")
                onSuccess(reqId)
            }.onFailure {
                emitToast("সাবমিট ব্যর্থ হয়েছে: ${it.message}")
            }
        }
    }

    fun fetchUserProfileAndQuota() {
        viewModelScope.launch {
            repository.fetchUserProfileAndQuota()
        }
    }

    fun saveUserApiKey(key: String, assignedNumber: String? = null) {
        repository.saveUserApiKey(key, assignedNumber)
        emitToast("API Key সংরক্ষিত হয়েছে")
        fetchUserProfileAndQuota()
    }

    // Admin Actions
    fun adminLogin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.adminLogin(pin)
            result.onSuccess {
                emitToast("এডমিন সফলভাবে লগইন হয়েছেন")
                loadAdminDashboard()
                onResult(true)
            }.onFailure {
                emitToast("এডমিন পিন সঠিক নয়")
                onResult(false)
            }
        }
    }

    fun loadAdminDashboard() {
        viewModelScope.launch {
            repository.getAdminRequests().onSuccess {
                _adminRequests.value = it
            }
            repository.getAdminUsers().onSuccess {
                _adminUsers.value = it
            }
            try {
                val statsRes = com.example.data.remote.ApiClient.getService().getAdminStats()
                if (statsRes.isSuccessful) {
                    _adminStats.value = statsRes.body()
                }
            } catch (_: Exception) {}
        }
    }

    fun approveRequest(id: String) {
        viewModelScope.launch {
            repository.approveRequest(id).onSuccess {
                emitToast("রিকোয়েস্ট অনুমোদন করা হয়েছে এবং API Key ইস্যু হয়েছে")
                loadAdminDashboard()
            }.onFailure {
                emitToast("অনুমোদন ব্যর্থ হয়েছে")
            }
        }
    }

    fun rejectRequest(id: String, note: String) {
        viewModelScope.launch {
            repository.rejectRequest(id, note).onSuccess {
                emitToast("রিকোয়েস্ট বাতিল করা হয়েছে")
                loadAdminDashboard()
            }.onFailure {
                emitToast("বাতিল ব্যর্থ হয়েছে")
            }
        }
    }

    fun updateAdminRates(rates: RatesAndBankingDto) {
        viewModelScope.launch {
            repository.updateAdminRates(rates).onSuccess {
                emitToast("রেট ও ব্যাংকিং নাম্বার সফলভাবে আপডেট হয়েছে")
            }.onFailure {
                emitToast("আপডেট ব্যর্থ হয়েছে")
            }
        }
    }

    fun topupUser(apiKey: String, addSms: Int, addMin: Int, assignedNum: String? = null) {
        viewModelScope.launch {
            repository.topupUser(apiKey, addSms, addMin, assignedNum).onSuccess {
                emitToast("ইউজার ব্যালেন্স সফলভাবে আপডেট হয়েছে")
                loadAdminDashboard()
            }.onFailure {
                emitToast("ব্যালেন্স আপডেট ব্যর্থ হয়েছে")
            }
        }
    }

    // Google Play In-App Billing
    val playBillingManager = PlayBillingManager(
        context = application,
        coroutineScope = viewModelScope,
        onPurchaseVerified = { sms, min, assignedNumber ->
            emitToast("Google Play Purchase Successful! Balance updated.")
            fetchUserProfileAndQuota()
        }
    )

    // Twilio Number Inventory & Marketplace Catalog
    val numberCatalog = repository.numberCatalog.asStateFlow()
    val myTwilioNumbers = repository.myTwilioNumbers.asStateFlow()
    val availableTwilioNumbers = repository.availableTwilioNumbers.asStateFlow()

    fun fetchNumberCatalog() {
        viewModelScope.launch {
            repository.fetchNumberCatalog()
        }
    }

    fun searchAvailableTwilioNumbers(country: String = "US") {
        viewModelScope.launch {
            repository.searchAvailableTwilioNumbers(country)
        }
    }

    fun buyTwilioNumber(phoneNumber: String?, friendlyName: String?, assignApiKey: String? = null, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.buyTwilioNumber(phoneNumber, friendlyName, assignApiKey).onSuccess { res ->
                emitToast("নম্বর সফলভাবে ক্রয় করা হয়েছে: ${res.phoneNumber}")
                res.phoneNumber?.let { onSuccess(it) }
                loadAdminDashboard()
            }.onFailure {
                emitToast("নম্বর ক্রয় ব্যর্থ: ${it.message}")
            }
        }
    }

    fun releaseTwilioNumber(sidOrNumber: String) {
        viewModelScope.launch {
            repository.releaseTwilioNumber(sidOrNumber).onSuccess {
                emitToast("নম্বর সফলভাবে বাতিল ও বিলিং বন্ধ করা হয়েছে")
                loadAdminDashboard()
            }.onFailure {
                emitToast("নম্বর বাতিল ব্যর্থ: ${it.message}")
            }
        }
    }

    fun fetchMyTwilioNumbers() {
        viewModelScope.launch {
            repository.fetchMyTwilioNumbers()
        }
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }
}
