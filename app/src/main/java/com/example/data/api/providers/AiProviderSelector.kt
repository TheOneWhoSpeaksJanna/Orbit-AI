package com.example.data.api.providers

import com.example.domain.api.AiProvider
import com.example.domain.api.AiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AiProviderSelector : AiProvider {
    
    private val providers = mutableMapOf<String, AiProvider>()

    init {
        providers["Gemini"] = GeminiProvider()
        providers["OpenAI"] = OpenAIProvider()
        providers["Claude"] = ClaudeProvider()
        providers["OpenRouter"] = OpenRouterProvider()
    }

    private fun getProvider(name: String): AiProvider {
        return providers[name] ?: providers["Gemini"]!!
    }

    override fun generateContentStream(prompt: String, apiKey: String, provider: String, model: String): Flow<AiResult> {
        return getProvider(provider).generateContentStream(prompt, apiKey, provider, model)
    }

    override suspend fun generateContent(prompt: String, apiKey: String, provider: String, model: String): AiResult {
        return getProvider(provider).generateContent(prompt, apiKey, provider, model)
    }

    override suspend fun testConnection(provider: String, apiKey: String, model: String): Boolean {
        return getProvider(provider).testConnection(provider, apiKey, model)
    }
}
