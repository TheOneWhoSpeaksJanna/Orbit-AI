package com.example.data.api.providers

import com.example.data.api.GeminiRequest
import com.example.data.api.GeminiResponse
import com.example.data.api.GeminiService
import com.example.domain.api.AiEvent
import com.example.domain.api.AiProvider
import com.example.domain.api.AiResult
import com.example.domain.api.ProviderMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi

class GeminiProvider : AiProvider {

    private val moshi = Moshi.Builder().build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val httpClient = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(httpClient)
        .build()

    private val service = retrofit.create(GeminiService::class.java)

    override fun generateContentStream(sessionId: String?, prompt: String, apiKey: String, provider: String, model: String): Flow<AiEvent> = flow {
        if (apiKey.isBlank()) {
            emit(AiEvent.Error("API Key is missing."))
            return@flow
        }
        val requestModel = if (model.isNotBlank()) model else "gemini-1.5-pro-latest"
        val jsonBody = JSONObject().apply {
            val contents = org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            }
            put("contents", contents)
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$requestModel:streamGenerateContent?alt=sse&key=$apiKey")
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val source = response.body?.source()
            if (source == null) {
                emit(AiEvent.Error("Empty response from Gemini."))
                return@flow
            }
            val fullText = StringBuilder()
            source.use { src ->
                while (true) {
                    val currentLine = src.readUtf8Line() ?: break
                    if (!currentLine.startsWith("data: ")) continue
                    val data = currentLine.removePrefix("data: ")
                    if (data.trim() == "[DONE]") break
                    try {
                        val json = JSONObject(data)
                        val candidates = json.optJSONArray("candidates")
                        if (candidates == null || candidates.length() == 0) continue
                        val content = candidates.getJSONObject(0).optJSONObject("content") ?: continue
                        val parts = content.optJSONArray("parts") ?: continue
                        for (i in 0 until parts.length()) {
                            val text = parts.getJSONObject(i).optString("text")
                            if (text.isNotEmpty()) {
                                fullText.append(text)
                                emit(AiEvent.Chunk(text))
                            }
                        }
                    } catch (_: Exception) { }
                }
            }
            emit(AiEvent.Done(fullText.toString()))
        } catch (e: Exception) {
            emit(AiEvent.Error(e.message ?: "Network error occurred"))
        }
    }

    override suspend fun generateContent(prompt: String, apiKey: String, provider: String, model: String): AiResult {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    return@withContext AiResult.Error("API Key is missing.")
                }
                val requestModel = if (model.isNotBlank()) model else "gemini-1.5-pro-latest"
                val request = GeminiRequest(
                    contents = listOf(GeminiRequest.Content(parts = listOf(GeminiRequest.Part(text = prompt))))
                )

                val response = service.generateContent(apiKey = apiKey, request = request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (text != null) {
                    AiResult.Success(text)
                } else {
                    AiResult.Error("No valid response from Gemini.")
                }
            } catch (e: Exception) {
                AiResult.Error(e.message ?: "Network error occurred")
            }
        }
    }

    override suspend fun createSession(sessionId: String, systemPrompt: String?) { }
    override suspend fun deleteSession(sessionId: String) { }

    override val metadata: ProviderMetadata = ProviderMetadata(
        name = "Gemini",
        displayName = "Google Gemini",
        models = listOf("gemini-1.5-pro-latest", "gemini-1.5-flash-latest", "gemini-2.0-flash-exp"),
        supportsStreaming = true,
        requiresApiKey = true,
        defaultModel = "gemini-1.5-pro-latest"
    )

    override suspend fun testConnection(provider: String, apiKey: String, model: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) return@withContext false
                val request = GeminiRequest(
                    contents = listOf(GeminiRequest.Content(parts = listOf(GeminiRequest.Part(text = "Hi"))))
                )
                service.generateContent(apiKey = apiKey, request = request)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
