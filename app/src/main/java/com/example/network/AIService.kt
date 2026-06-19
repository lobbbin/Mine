package com.example.network

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

// --- OpenAI Data Classes ---
@JsonClass(generateAdapter = true)
data class OpenAIMessage(
    val role: String,
    val content: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenAIResponseFormat(
    val type: String
)

@JsonClass(generateAdapter = true)
data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val response_format: OpenAIResponseFormat? = null,
    val temperature: Double? = null,
    val max_tokens: Int? = null,
    val top_p: Double? = null,
    val stream: Boolean = false,
    val reasoning_effort: String? = null,
    val chat_template_kwargs: Map<String, Boolean>? = null,
    val frequency_penalty: Double? = null,
    val presence_penalty: Double? = null
)

@JsonClass(generateAdapter = true)
data class OpenAIResponse(
    val choices: List<OpenAIChoice>
)

@JsonClass(generateAdapter = true)
data class OpenAIChoice(
    val message: OpenAIMessage
)

// --- Anthropic Data Classes ---
@JsonClass(generateAdapter = true)
data class AnthropicMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class AnthropicRequest(
    val model: String,
    val max_tokens: Int = 4000,
    val system: String? = null,
    val messages: List<AnthropicMessage>,
    val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicResponse(
    val content: List<AnthropicContentPart>
)

@JsonClass(generateAdapter = true)
data class AnthropicContentPart(
    val type: String,
    val text: String
)

// --- Gemini Data Classes ---
@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val functionCall: GeminiFunctionCall? = null,
    val functionResponse: GeminiFunctionResponse? = null
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, Any>
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionResponse(
    val name: String,
    val response: Map<String, Any>
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiSystemInstruction(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val maxOutputTokens: Int? = null,
    val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null,
    val generationConfig: GeminiGenerationConfig? = null,
    val tools: List<GeminiTool>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiTool(
    val functionDeclarations: List<GeminiFunctionDeclaration>
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>? = null // Usually a JSON Schema object
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent
)

// --- Retrofit Interface ---
interface AIService {

    @POST
    suspend fun callOpenAI(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/json",
        @Body body: OpenAIRequest
    ): OpenAIResponse

    @Streaming
    @POST
    suspend fun callOpenAIStream(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "text/event-stream",
        @Body body: OpenAIRequest
    ): okhttp3.ResponseBody

    @POST
    suspend fun callAnthropic(
        @Url url: String,
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String,
        @Header("content-type") contentType: String = "application/json",
        @Body body: AnthropicRequest
    ): AnthropicResponse

    @POST
    suspend fun callGemini(
        @Url url: String,
        @Body body: GeminiRequest
    ): GeminiResponse
}

// --- Direct Test Connection Response ---
// Simple dummy structure to see if some response returns
@JsonClass(generateAdapter = true)
data class PingResponse(
    val status: String? = null
)

object RetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.MINUTES)
        .readTimeout(2, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: AIService by lazy {
        Retrofit.Builder()
            // We can put any dummy base url as calls use @Url which overrides it
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AIService::class.java)
    }
}
