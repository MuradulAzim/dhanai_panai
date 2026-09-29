package com.example.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ServerStatusResponse(
    @Json(name = "status") val status: String,
    @Json(name = "twilioConfigured") val twilioConfigured: Boolean,
    @Json(name = "twilioPhoneNumber") val twilioPhoneNumber: String?,
    @Json(name = "activeCallsCount") val activeCallsCount: Int = 0,
    @Json(name = "serverTime") val serverTime: String? = null
)

@JsonClass(generateAdapter = true)
data class OutboundCallRequest(
    @Json(name = "to") val to: String,
    @Json(name = "record") val record: Boolean = false
)

@JsonClass(generateAdapter = true)
data class OutboundCallResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "callSid") val callSid: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class TerminateCallRequest(
    @Json(name = "callSid") val callSid: String
)

@JsonClass(generateAdapter = true)
data class TerminateCallResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "status") val status: String?
)

@JsonClass(generateAdapter = true)
data class SendSmsRequest(
    @Json(name = "to") val to: String,
    @Json(name = "body") val body: String
)

@JsonClass(generateAdapter = true)
data class SendSmsResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "messageSid") val messageSid: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "to") val to: String?,
    @Json(name = "from") val from: String?,
    @Json(name = "body") val body: String?,
    @Json(name = "errorMessage") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class CallDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "callSid") val callSid: String,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "direction") val direction: String,
    @Json(name = "status") val status: String,
    @Json(name = "duration") val duration: Int? = 0,
    @Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "messageSid") val messageSid: String?,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "body") val body: String,
    @Json(name = "direction") val direction: String,
    @Json(name = "status") val status: String,
    @Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SimulateEventRequest(
    @Json(name = "type") val type: String, // "call" or "sms"
    @Json(name = "from") val from: String,
    @Json(name = "body") val body: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateTwilioCredentialsRequest(
    @Json(name = "accountSid") val accountSid: String,
    @Json(name = "authToken") val authToken: String,
    @Json(name = "phoneNumber") val phoneNumber: String
)

@JsonClass(generateAdapter = true)
data class UpdateTwilioCredentialsResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "twilioPhoneNumber") val twilioPhoneNumber: String?,
    @Json(name = "twilioConfigured") val twilioConfigured: Boolean
)

@JsonClass(generateAdapter = true)
data class GenericApiResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?
)
