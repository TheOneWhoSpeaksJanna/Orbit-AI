package com.example.data.api.providers

import com.example.domain.api.AiProvider
import com.example.domain.api.AiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenRouterProvider : AiProvider {

    private val httpClient = OkHttpClient.Builder().build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override fun generateContentStream(prompt: String, apiKey: String, provider: String, model: String): Flow<AiResult> = flow {
        emit(generateContent(prompt, apiKey, provider, model))
    }

    override suspend fun generateContent(prompt: String, apiKey: String, provider: String, model: String): AiResult {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) return@withContext AiResult.Error("API Key is missing.")

                val requestModel = if (model.isNotBlank()) model else "anthropic/claude-3-opus"
                
                val jsonBody = JSONObject().apply {
                    put("model", requestModel)
                    val messages = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    }
                    put("messages", messages)
                }

                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .post(jsonBody.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val choices = jsonResponse.optJSONArray("choices")
                    val text = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                    
                    if (!text.isNullOrBlank()) {
                        AiResult.Success(text)
                    } else {
                        AiResult.Error("Empty response from OpenRouter.")
                    }
                } else {
                    AiResult.Error("HTTP Error ${response.code}: ${responseBody ?: response.message}")
                }
            } catch (e: Exception) {
                AiResult.Error("Network Error: ${e.message}")
            }
        }
    }

    override suspend fun testConnection(provider: String, apiKey: String, model: String): Boolean {
        return withContext(Dispatchers.IO) {
            val result = generateContent("Hi", apiKey, provider, model)
            result is AiResult.Success
        }
    }
}
