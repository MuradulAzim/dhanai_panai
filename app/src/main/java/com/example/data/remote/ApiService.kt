package com.example.data.remote

import com.example.data.remote.models.CallDto
import com.example.data.remote.models.GenericApiResponse
import com.example.data.remote.models.MessageDto
import com.example.data.remote.models.OutboundCallRequest
import com.example.data.remote.models.OutboundCallResponse
import com.example.data.remote.models.SendSmsRequest
import com.example.data.remote.models.SendSmsResponse
import com.example.data.remote.models.ServerStatusResponse
import com.example.data.remote.models.SimulateEventRequest
import com.example.data.remote.models.TerminateCallRequest
import com.example.data.remote.models.TerminateCallResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("api/status")
    suspend fun getStatus(): Response<ServerStatusResponse>

    @POST("api/calls/outbound")
    suspend fun makeOutboundCall(
        @Body request: OutboundCallRequest
    ): Response<OutboundCallResponse>

    @POST("api/calls/terminate")
    suspend fun terminateCall(
        @Body request: TerminateCallRequest
    ): Response<TerminateCallResponse>

    @GET("api/calls")
    suspend fun getCallHistory(
        @Query("limit") limit: Int = 50
    ): Response<List<CallDto>>

    @POST("api/messages/send")
    suspend fun sendSms(
        @Body request: SendSmsRequest
    ): Response<SendSmsResponse>

    @GET("api/messages")
    suspend fun getMessages(
        @Query("limit") limit: Int = 100
    ): Response<List<MessageDto>>

    @GET("api/conversations/{number}/messages")
    suspend fun getConversationMessages(
        @Path("number") phoneNumber: String
    ): Response<List<MessageDto>>

    @POST("api/messages/mark-read")
    suspend fun markMessagesRead(
        @Body payload: Map<String, String>
    ): Response<GenericApiResponse>

    @POST("api/simulate")
    suspend fun simulateEvent(
        @Body request: SimulateEventRequest
    ): Response<GenericApiResponse>

    @POST("api/settings/twilio")
    suspend fun updateTwilioCredentials(
        @Body request: com.example.data.remote.models.UpdateTwilioCredentialsRequest
    ): Response<com.example.data.remote.models.UpdateTwilioCredentialsResponse>
}
