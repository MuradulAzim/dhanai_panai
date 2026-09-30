package com.example.data.remote.models

data class RatesAndBankingDto(
    val pricePerSmsBdt: Double = 2.0,
    val pricePerCallMinuteBdt: Double = 5.0,
    val bkashNumber: String = "018XXXXXXXX",
    val nagadNumber: String = "017XXXXXXXX",
    val rocketNumber: String = "019XXXXXXXX",
    val adminPin: String? = null
)

data class UserPackageRequest(
    val userName: String,
    val userContact: String,
    val smsRequested: Int,
    val minutesRequested: Int,
    val totalAmountBdt: Double,
    val paymentMethod: String,
    val senderNumber: String,
    val transactionId: String,
    val screenshotBase64: String? = null
)

data class UserPackageResponse(
    val success: Boolean,
    val message: String? = null,
    val request: RechargeRequestDto? = null
)

data class RechargeRequestDto(
    val id: String,
    val userName: String,
    val userContact: String,
    val smsRequested: Int,
    val minutesRequested: Int,
    val totalAmountBdt: Double,
    val paymentMethod: String,
    val senderNumber: String,
    val transactionId: String,
    val screenshotBase64: String? = null,
    val status: String,
    val assignedApiKey: String? = null,
    val assignedPhoneNumber: String? = null,
    val createdAt: String,
    val reviewedAt: String? = null,
    val adminNote: String? = null
)

data class AdminRequestsResponse(
    val requests: List<RechargeRequestDto> = emptyList()
)

data class AdminStatsDto(
    val totalUsers: Int = 0,
    val pendingRequests: Int = 0,
    val approvedRequests: Int = 0,
    val totalRevenueBdt: Double = 0.0,
    val activeTwilioNumber: String? = null
)

data class ApiUserDto(
    val apiKey: String,
    val name: String,
    val contactNumber: String,
    val assignedNumber: String,
    val smsBalance: Int,
    val callMinutesBalance: Int,
    val status: String,
    val createdAt: String,
    val totalSmsSent: Int = 0,
    val totalCallMinutesUsed: Int = 0
)

data class AdminUsersResponse(
    val users: List<ApiUserDto> = emptyList()
)

data class NumberPackageDto(
    val id: String,
    val title: String,
    val country: String,
    val countryCode: String,
    val flag: String,
    val sampleNumber: String,
    val category: String, // CALL_AND_SMS, WHATSAPP_TELEGRAM, BURNER_OTP
    val typeLabel: String,
    val description: String,
    val priceBdt: Double,
    val priceUsd: Double,
    val durationDays: Int,
    val smsQuota: Int,
    val callMinutesQuota: Int,
    val supportsWhatsApp: Boolean = true,
    val supportsGoogleOtp: Boolean = true,
    val supportsTelegram: Boolean = true
)

data class NumberCatalogResponse(
    val success: Boolean,
    val catalog: List<NumberPackageDto> = emptyList()
)

data class AvailableTwilioNumberDto(
    val phoneNumber: String,
    val friendlyName: String,
    val locality: String? = null,
    val region: String? = null,
    val isoCountry: String,
    val capabilities: TwilioCapabilitiesDto? = null
)

data class TwilioCapabilitiesDto(
    val voice: Boolean = true,
    val SMS: Boolean = true,
    val MMS: Boolean = false
)

data class AvailableNumbersResponse(
    val success: Boolean,
    val country: String = "US",
    val numbers: List<AvailableTwilioNumberDto> = emptyList()
)

data class ActiveTwilioNumberDto(
    val sid: String,
    val phoneNumber: String,
    val friendlyName: String,
    val dateCreated: String,
    val capabilities: Map<String, Boolean>? = null
)

data class MyTwilioNumbersResponse(
    val success: Boolean,
    val total: Int = 0,
    val numbers: List<ActiveTwilioNumberDto> = emptyList()
)

data class BuyNumberRequest(
    val phoneNumber: String? = null,
    val friendlyName: String? = null,
    val assignToApiKey: String? = null
)

data class BuyNumberResponse(
    val success: Boolean,
    val message: String? = null,
    val phoneNumber: String? = null,
    val sid: String? = null
)

data class ReleaseNumberRequest(
    val sid: String? = null,
    val phoneNumber: String? = null
)

data class PlayPurchaseVerifyRequest(
    val productId: String,
    val purchaseToken: String,
    val orderId: String? = null,
    val apiKey: String? = null
)

data class PlayPurchaseVerifyResponse(
    val success: Boolean,
    val message: String? = null,
    val apiKey: String? = null,
    val assignedNumber: String? = null,
    val smsBalance: Int = 0,
    val callMinutesBalance: Int = 0
)

data class UserProfileResponse(
    val success: Boolean,
    val user: ApiUserDto? = null,
    val message: String? = null
)

data class AdminLoginRequest(
    val pin: String
)

data class AdminLoginResponse(
    val success: Boolean,
    val message: String? = null,
    val token: String? = null
)
