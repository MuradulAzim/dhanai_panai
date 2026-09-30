package com.example.data.remote.gemini

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class GeminiModel(val modelId: String, val displayName: String, val description: String) {
    FLASH("gemini-3.5-flash", "Gemini 3.5 Flash", "General tasks & smart reasoning (Recommended)"),
    PRO("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Complex reasoning, coding & analysis"),
    FLASH_LITE("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite", "Ultra-fast replies & low latency")
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ChatbotRole(
    val title: String,
    val systemInstruction: String
) {
    TELECOM_ADVISOR(
        "Telecom & Privacy Advisor",
        "You are the Second Number Smart Telecom Assistant. You are an expert in virtual phone numbers, VoIP, Twilio telephony, SMS protocols, and privacy protection. Guide the user with warm, professional, and clear advice. You can respond in English or Bengali depending on the user's language."
    ),
    SMS_DRAFTER(
        "Business SMS & Reply Drafter",
        "You are an expert copywriter specializing in drafting concise, effective, polite, and persuasive SMS messages, business proposals, payment reminders, and customer follow-up texts. Provide ready-to-copy drafts with character counts."
    ),
    CALL_SCRIPT_WRITER(
        "Call Script & Support Assistant",
        "You are an expert sales and customer support call coach. You help write clear phone call scripts, objection handling responses, and friendly conversational talking points for outbound and inbound calls."
    ),
    GENERAL_ASSISTANT(
        "General AI Assistant",
        "You are a helpful, knowledgeable, and polite AI assistant powered by Google Gemini. Answer any questions clearly, concisely, and accurately in the user's language."
    )
}

object GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateChatResponse(
        history: List<ChatMessage>,
        userMessage: String,
        model: GeminiModel = GeminiModel.FLASH,
        role: ChatbotRole = ChatbotRole.TELECOM_ADVISOR
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                // If API key is placeholder, provide a helpful demo fallback response
                return@withContext Result.success(
                    "🤖 [Gemini AI Assistant]: " +
                    "আপনার প্রশ্ন: \"$userMessage\"\n\n" +
                    "আমি আপনার সেকেন্ড নম্বর ভার্চুয়াল টেলিফোনি অ্যাসিস্ট্যান্ট। " +
                    "আমেরিকান/ইউকে ভার্চুয়াল নম্বর ব্যবহার করে সহজে বিশ্বব্যাপী ভয়েস কল ও এসএমএস পাঠানো যায়। " +
                    "(রিয়েল টাইমে জেমেনাই সক্রিয় করতে AI Studio Secrets প্যানেলে আপনার GEMINI_API_KEY প্রদান করুন)।"
                )
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.modelId}:generateContent?key=$apiKey"

            val jsonBody = JSONObject()

            // System Instruction
            val sysInstructionObj = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", role.systemInstruction))
            sysInstructionObj.put("parts", sysParts)
            jsonBody.put("systemInstruction", sysInstructionObj)

            // Contents array (Conversation History + Current Message)
            val contentsArray = JSONArray()

            // Include last 10 turns of history for context
            val contextHistory = history.takeLast(10)
            for (msg in contextHistory) {
                val turn = JSONObject()
                turn.put("role", if (msg.role == "user") "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", msg.text))
                turn.put("parts", parts)
                contentsArray.put(turn)
            }

            // Current user message
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()
            currentParts.put(JSONObject().put("text", userMessage))
            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)

            jsonBody.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            jsonBody.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: "API Error ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No response generated by model"))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (text.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Empty text in Gemini response"))
            }

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
