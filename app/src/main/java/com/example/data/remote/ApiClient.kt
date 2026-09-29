package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Default fallback pointing to standard emulator localhost (10.0.2.2:3000) or user configurable
    private var baseUrl: String = "http://10.0.2.2:3000/"
    private var authToken: String = "test-secret-token"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
        if (authToken.isNotBlank()) {
            builder.header("Authorization", "Bearer $authToken")
            builder.header("x-api-key", authToken)
        }
        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private var okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private var retrofit: Retrofit = buildRetrofit()
    private var apiService: ApiService = retrofit.create(ApiService::class.java)

    private fun buildRetrofit(): Retrofit {
        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(cleanUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Synchronized
    fun updateConfig(newBaseUrl: String, newAuthToken: String) {
        var formattedUrl = newBaseUrl.trim()
        if (formattedUrl.isNotBlank() && !formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "http://$formattedUrl"
        }
        if (formattedUrl.isNotBlank() && !formattedUrl.endsWith("/")) {
            formattedUrl = "$formattedUrl/"
        }
        if (formattedUrl.isNotBlank()) {
            baseUrl = formattedUrl
        }
        authToken = newAuthToken.trim()
        retrofit = buildRetrofit()
        apiService = retrofit.create(ApiService::class.java)
    }

    fun getService(): ApiService = apiService

    fun getBaseUrl(): String = baseUrl
    fun getAuthToken(): String = authToken
}
