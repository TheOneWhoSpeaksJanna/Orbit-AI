package com.omniclaw.domain.api

import kotlinx.coroutines.flow.Flow

interface AiProvider {
    fun generateContentStream(
        sessionId: String? = null,
        prompt: String,
        apiKey: String,
        provider: String = "Gemini",
        model: String = ""
    ): Flow<AiEvent>

    suspend fun generateContent(
        prompt: String,
        apiKey: String,
        provider: String = "Gemini",
        model: String = ""
    ): AiResult

    suspend fun testConnection(provider: String, apiKey: String, model: String): Boolean

    suspend fun createSession(sessionId: String, systemPrompt: String? = null)
    suspend fun deleteSession(sessionId: String)

    val metadata: ProviderMetadata
}

sealed class AiResult {
    data class Success(val text: String) : AiResult()
    data class Error(val message: String) : AiResult()
}
