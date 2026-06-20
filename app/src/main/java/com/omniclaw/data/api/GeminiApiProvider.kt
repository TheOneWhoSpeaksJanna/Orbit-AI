package com.omniclaw.data.api

import com.omniclaw.domain.api.AiProvider
import com.omniclaw.domain.api.AiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class GeminiRequest(val contents: List<Content>) {
    @JsonClass(generateAdapter = true)
    data class Content(val parts: List<Part>)
    @JsonClass(generateAdapter = true)
    data class Part(val text: String)
}

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<Candidate>?) {
    @JsonClass(generateAdapter = true)
    data class Candidate(val content: Content?)
    @JsonClass(generateAdapter = true)
    data class Content(val parts: List<Part>?)
    @JsonClass(generateAdapter = true)
    data class Part(val text: String?)
}

interface GeminiService {
    @POST("v1beta/models/gemini-1.5-pro-latest:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: GeminiRequest
    ): GeminiResponse
}

// Provider removed
