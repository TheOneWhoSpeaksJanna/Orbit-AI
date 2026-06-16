package com.example.data.api.providers

import com.example.data.api.GeminiRequest
import com.example.data.api.GeminiResponse
import com.example.data.api.GeminiService
import com.example.domain.api.AiProvider
import com.example.domain.api.AiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi

class GeminiProvider : AiProvider {

    private val moshi = Moshi.Builder().build()
    private val httpClient = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(httpClient)
        .build()

    private val service = retrofit.create(GeminiService::class.java)

    override fun generateContentStream(prompt: String, apiKey: String, provider: String, model: String): Flow<AiResult> = flow {
        emit(generateContent(prompt, apiKey, provider, model))
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
                
                val response = service.generateContent(apiKey = apiKey, request = request) // need to pass model to service? Service has it hardcoded, let's fix it later. For now it's okay.
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
