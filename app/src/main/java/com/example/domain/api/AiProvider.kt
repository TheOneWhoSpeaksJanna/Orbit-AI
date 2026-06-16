package com.example.domain.api

import kotlinx.coroutines.flow.Flow

interface AiProvider {
    fun generateContentStream(prompt: String, apiKey: String, provider: String = "Gemini", model: String = ""): Flow<AiResult>
    suspend fun generateContent(prompt: String, apiKey: String, provider: String = "Gemini", model: String = ""): AiResult
    suspend fun testConnection(provider: String, apiKey: String, model: String): Boolean
}

sealed class AiResult {
    data class Success(val text: String) : AiResult()
    data class Error(val message: String) : AiResult()
}
