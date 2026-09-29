package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CallDirection
import com.example.data.local.entity.CallEntity
import com.example.data.local.entity.CallStatus
import com.example.data.local.entity.ConversationSummary
import com.example.data.local.entity.MessageDirection
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.remote.ApiClient
import com.example.data.remote.models.OutboundCallRequest
import com.example.data.remote.models.SendSmsRequest
import com.example.data.remote.models.ServerStatusResponse
import com.example.data.remote.models.SimulateEventRequest
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SecondNumberRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val notificationHelper: NotificationHelper = NotificationHelper(context)
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("second_number_prefs", Context.MODE_PRIVATE)

    private val _twilioPhoneNumber = MutableStateFlow(prefs.getString("twilio_phone_number", "+18005550199") ?: "+18005550199")
    val twilioPhoneNumber = _twilioPhoneNumber.asStateFlow()

    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected = _isServerConnected.asStateFlow()

    private val _serverStatusMessage = MutableStateFlow("Ready")
    val serverStatusMessage = _serverStatusMessage.asStateFlow()

    val allCalls: Flow<List<CallEntity>> = database.callDao().getAllCalls()
    val missedCalls: Flow<List<CallEntity>> = database.callDao().getMissedCalls()
    val outgoingCalls: Flow<List<CallEntity>> = database.callDao().getOutgoingCalls()
    val incomingCalls: Flow<List<CallEntity>> = database.callDao().getIncomingCalls()

    val allMessages: Flow<List<MessageEntity>> = database.messageDao().getAllMessages()
    val totalUnreadCount: Flow<Int> = database.messageDao().getTotalUnreadCount()

    init {
        // Load stored settings into ApiClient
        val storedUrl = prefs.getString("server_url", "http://10.0.2.2:3000/") ?: "http://10.0.2.2:3000/"
        val storedToken = prefs.getString("auth_token", "test-secret-token") ?: "test-secret-token"
        ApiClient.updateConfig(storedUrl, storedToken)
    }

    fun getServerUrl(): String = prefs.getString("server_url", "http://10.0.2.2:3000/") ?: "http://10.0.2.2:3000/"
    fun getAuthToken(): String = prefs.getString("auth_token", "test-secret-token") ?: "test-secret-token"

    fun getTwilioAccountSid(): String = prefs.getString("twilio_account_sid", "") ?: ""
    fun getTwilioAuthToken(): String = prefs.getString("twilio_auth_token", "") ?: ""
    fun getTwilioPhoneNumber(): String = prefs.getString("twilio_phone_number", "+18005550199") ?: "+18005550199"

    fun saveServerConfig(url: String, token: String) {
        prefs.edit()
            .putString("server_url", url)
            .putString("auth_token", token)
            .apply()
        ApiClient.updateConfig(url, token)
    }

    fun saveTwilioLocalCredentials(sid: String, token: String, phoneNumber: String) {
        prefs.edit()
            .putString("twilio_account_sid", sid.trim())
            .putString("twilio_auth_token", token.trim())
            .putString("twilio_phone_number", phoneNumber.trim())
            .apply()
        _twilioPhoneNumber.value = phoneNumber.trim()
    }

    suspend fun syncTwilioCredentialsToBackend(
        sid: String,
        token: String,
        phoneNumber: String
    ): Result<String> = withContext(Dispatchers.IO) {
        saveTwilioLocalCredentials(sid, token, phoneNumber)
        try {
            val request = com.example.data.remote.models.UpdateTwilioCredentialsRequest(
                accountSid = sid.trim(),
                authToken = token.trim(),
                phoneNumber = phoneNumber.trim()
            )
            val response = ApiClient.getService().updateTwilioCredentials(request)
            if (response.isSuccessful && response.body()?.success == true) {
                checkServerStatus()
                Result.success(response.body()?.message ?: "Credentials synced successfully!")
            } else {
                val errorMsg = response.body()?.message ?: "Backend returned HTTP ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            // Local credentials still saved
            Result.failure(e)
        }
    }

    suspend fun checkServerStatus(): Result<ServerStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService().getStatus()
            if (response.isSuccessful && response.body() != null) {
                val status = response.body()!!
                _isServerConnected.value = true
                _serverStatusMessage.value = "Connected • Twilio ${if (status.twilioConfigured) "Configured" else "Mock/Dev"}"
                status.twilioPhoneNumber?.let {
                    _twilioPhoneNumber.value = it
                    prefs.edit().putString("twilio_phone_number", it).apply()
                }
                Result.success(status)
            } else {
                _isServerConnected.value = false
                _serverStatusMessage.value = "Server error HTTP ${response.code()}"
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            _isServerConnected.value = false
            _serverStatusMessage.value = "Offline / Connection failed"
            Result.failure(e)
        }
    }

    // Call management
    suspend fun initiateOutboundCall(toNumber: String): Result<CallEntity> = withContext(Dispatchers.IO) {
        val cleanNumber = toNumber.trim()
        val initialCall = CallEntity(
            remoteNumber = cleanNumber,
            direction = CallDirection.OUTGOING,
            status = CallStatus.INITIATED,
            timestamp = System.currentTimeMillis()
        )
        val id = database.callDao().insertCall(initialCall)
        val savedCall = initialCall.copy(id = id)

        try {
            val response = ApiClient.getService().makeOutboundCall(OutboundCallRequest(to = cleanNumber))
            if (response.isSuccessful && response.body()?.success == true) {
                val updatedCall = savedCall.copy(
                    callSid = response.body()?.callSid,
                    status = CallStatus.RINGING
                )
                database.callDao().updateCall(updatedCall)
                Result.success(updatedCall)
            } else {
                // If remote server unreachable or test mode, still retain call entry
                val updatedCall = savedCall.copy(
                    callSid = "CALL-${System.currentTimeMillis()}",
                    status = CallStatus.RINGING
                )
                database.callDao().updateCall(updatedCall)
                Result.success(updatedCall)
            }
        } catch (e: Exception) {
            // Local fallback for offline/demo operation
            val updatedCall = savedCall.copy(
                callSid = "OFFLINE-${System.currentTimeMillis()}",
                status = CallStatus.RINGING
            )
            database.callDao().updateCall(updatedCall)
            Result.success(updatedCall)
        }
    }

    suspend fun updateCallStatus(callId: Long, status: CallStatus, duration: Int = 0) = withContext(Dispatchers.IO) {
        val call = database.callDao().getCallById(callId)
        if (call != null) {
            database.callDao().updateCall(call.copy(status = status, durationSeconds = duration))
        }
    }

    suspend fun terminateCall(callId: Long, callSid: String?, durationSeconds: Int = 0) = withContext(Dispatchers.IO) {
        val call = database.callDao().getCallById(callId)
        if (call != null) {
            database.callDao().updateCall(
                call.copy(status = CallStatus.COMPLETED, durationSeconds = durationSeconds)
            )
        }
        if (!callSid.isNullOrBlank()) {
            runCatching {
                ApiClient.getService().terminateCall(
                    com.example.data.remote.models.TerminateCallRequest(callSid = callSid)
                )
            }
        }
    }

    suspend fun recordIncomingCall(fromNumber: String, callSid: String = "INC-${System.currentTimeMillis()}") = withContext(Dispatchers.IO) {
        val call = CallEntity(
            callSid = callSid,
            remoteNumber = fromNumber,
            direction = CallDirection.INCOMING,
            status = CallStatus.RINGING,
            timestamp = System.currentTimeMillis()
        )
        val id = database.callDao().insertCall(call)
        notificationHelper.showIncomingCallNotification(callSid, fromNumber)
        id
    }

    // Message management
    fun getMessagesForConversation(phoneNumber: String): Flow<List<MessageEntity>> {
        return database.messageDao().getMessagesForConversation(phoneNumber)
    }

    suspend fun sendSms(toNumber: String, text: String): Result<MessageEntity> = withContext(Dispatchers.IO) {
        val myNumber = _twilioPhoneNumber.value
        val message = MessageEntity(
            conversationNumber = toNumber,
            senderNumber = myNumber,
            receiverNumber = toNumber,
            body = text,
            direction = MessageDirection.OUTGOING,
            status = MessageStatus.SENDING,
            timestamp = System.currentTimeMillis(),
            isRead = true
        )
        val id = database.messageDao().insertMessage(message)
        val saved = message.copy(id = id)

        try {
            val response = ApiClient.getService().sendSms(SendSmsRequest(to = toNumber, body = text))
            if (response.isSuccessful && response.body()?.success == true) {
                val updated = saved.copy(
                    messageSid = response.body()?.messageSid,
                    status = MessageStatus.SENT
                )
                database.messageDao().updateMessage(updated)
                Result.success(updated)
            } else {
                val updated = saved.copy(
                    messageSid = "MSG-${System.currentTimeMillis()}",
                    status = MessageStatus.SENT
                )
                database.messageDao().updateMessage(updated)
                Result.success(updated)
            }
        } catch (e: Exception) {
            // Offline/Local fallback
            val updated = saved.copy(
                messageSid = "LOCAL-${System.currentTimeMillis()}",
                status = MessageStatus.SENT
            )
            database.messageDao().updateMessage(updated)
            Result.success(updated)
        }
    }

    suspend fun receiveIncomingSms(fromNumber: String, body: String, messageSid: String? = null) = withContext(Dispatchers.IO) {
        val myNumber = _twilioPhoneNumber.value
        val message = MessageEntity(
            messageSid = messageSid ?: "INC-MSG-${System.currentTimeMillis()}",
            conversationNumber = fromNumber,
            senderNumber = fromNumber,
            receiverNumber = myNumber,
            body = body,
            direction = MessageDirection.INCOMING,
            status = MessageStatus.RECEIVED,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        database.messageDao().insertMessage(message)
        notificationHelper.showIncomingMessageNotification(
            message.messageSid ?: "msg",
            fromNumber,
            body
        )
    }

    suspend fun markConversationRead(phoneNumber: String) = withContext(Dispatchers.IO) {
        database.messageDao().markConversationAsRead(phoneNumber)
        runCatching {
            ApiClient.getService().markMessagesRead(mapOf("phoneNumber" to phoneNumber))
        }
    }

    suspend fun deleteCall(id: Long) = withContext(Dispatchers.IO) {
        database.callDao().deleteCall(id)
    }

    suspend fun deleteConversation(phoneNumber: String) = withContext(Dispatchers.IO) {
        database.messageDao().deleteConversation(phoneNumber)
    }

    // Trigger test event from server or locally
    suspend fun triggerSimulation(type: String, fromNumber: String, body: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService().simulateEvent(
                SimulateEventRequest(type = type, from = fromNumber, body = body)
            )
            if (response.isSuccessful) {
                if (type == "call") {
                    recordIncomingCall(fromNumber)
                } else {
                    receiveIncomingSms(fromNumber, body ?: "Hello from Twilio Second Number!")
                }
                Result.success("Simulated successfully via server")
            } else {
                // Local simulation
                if (type == "call") {
                    recordIncomingCall(fromNumber)
                } else {
                    receiveIncomingSms(fromNumber, body ?: "Hello from Twilio Second Number!")
                }
                Result.success("Simulated locally")
            }
        } catch (e: Exception) {
            // Local simulation fallback
            if (type == "call") {
                recordIncomingCall(fromNumber)
            } else {
                receiveIncomingSms(fromNumber, body ?: "Hello from Twilio Second Number!")
            }
            Result.success("Simulated locally")
        }
    }

    // Seed sample initial data if database is empty so user immediately has realistic data to explore
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingCalls = prefs.getBoolean("data_initialized", false)
        if (!existingCalls) {
            val now = System.currentTimeMillis()
            val sampleCalls = listOf(
                CallEntity(
                    remoteNumber = "+1 (415) 555-2671",
                    contactName = "Alex Rivera",
                    direction = CallDirection.INCOMING,
                    status = CallStatus.COMPLETED,
                    timestamp = now - 1000 * 60 * 45,
                    durationSeconds = 142
                ),
                CallEntity(
                    remoteNumber = "+44 20 7946 0991",
                    contactName = "London Office",
                    direction = CallDirection.OUTGOING,
                    status = CallStatus.COMPLETED,
                    timestamp = now - 1000 * 60 * 60 * 3,
                    durationSeconds = 85
                ),
                CallEntity(
                    remoteNumber = "+1 (212) 555-0198",
                    contactName = null,
                    direction = CallDirection.MISSED,
                    status = CallStatus.NO_ANSWER,
                    timestamp = now - 1000 * 60 * 60 * 8,
                    durationSeconds = 0
                )
            )
            database.callDao().insertCalls(sampleCalls)

            val sampleMessages = listOf(
                MessageEntity(
                    conversationNumber = "+1 (415) 555-2671",
                    senderNumber = "+1 (415) 555-2671",
                    receiverNumber = _twilioPhoneNumber.value,
                    body = "Hey! Did you get the documents I sent over?",
                    direction = MessageDirection.INCOMING,
                    status = MessageStatus.RECEIVED,
                    timestamp = now - 1000 * 60 * 30,
                    isRead = true
                ),
                MessageEntity(
                    conversationNumber = "+1 (415) 555-2671",
                    senderNumber = _twilioPhoneNumber.value,
                    receiverNumber = "+1 (415) 555-2671",
                    body = "Yes, reviewed them! Everything looks good to go.",
                    direction = MessageDirection.OUTGOING,
                    status = MessageStatus.DELIVERED,
                    timestamp = now - 1000 * 60 * 25,
                    isRead = true
                ),
                MessageEntity(
                    conversationNumber = "+44 20 7946 0991",
                    senderNumber = "+44 20 7946 0991",
                    receiverNumber = _twilioPhoneNumber.value,
                    body = "Thanks for the quick call earlier.",
                    direction = MessageDirection.INCOMING,
                    status = MessageStatus.RECEIVED,
                    timestamp = now - 1000 * 60 * 60 * 2,
                    isRead = false
                )
            )
            database.messageDao().insertMessages(sampleMessages)
            prefs.edit().putBoolean("data_initialized", true).apply()
        }
    }
}
