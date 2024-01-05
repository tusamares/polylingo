package com.polylingo.data

import android.content.Context
import android.content.pm.PackageManager
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit


interface ChatGPTAPI {
    @POST("v1/chat/completions")
    @Headers("Content-Type: application/json")
    fun generateText(@Body request: GenerateTextRequest): Call<CompletionResponse>
}

data class CompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val usage: Usage,
    val choices: List<Choice>
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class Choice(
    val message: Message,
    val finish_reason: String,
    val index: Int
)

data class Message(
    val role: String,
    val content: String
)


interface ChatGPTCallback {
    fun onSuccess(responseText: String)
    fun onFailure(errorMessage: String)
}

data class GenerateTextRequest(
    val model: String,
    val messages: List<Map<String, String>>,
    val max_tokens: Int,
    val temperature: Double,
)

fun getApiKey(context: Context): Pair<String?, Int> {
    try {
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val apiKey = appInfo.metaData.getString("com.polylingo.api_key")
        if (apiKey.isNullOrEmpty()) {
            return Pair(null, 401) // Return null and error code 401 for authentication error
        }
        return Pair(apiKey, 200) // Return API key and success code 200
    } catch (e: Exception) {
        return Pair(null, 500) // Return null and error code 500 for server error
    }
}

fun getChatGPTAPI(apiKey: String): ChatGPTAPI {
    val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val builder = originalRequest.newBuilder()
                .header("Authorization", "Bearer $apiKey")
            val newRequest = builder.build()
            chain.proceed(newRequest)
        }
        .build()

    return Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ChatGPTAPI::class.java)
}


fun callChatGPT(context: Context, prompt: String, callback: ChatGPTCallback) {
    val apiKeyResult = getApiKey(context)
    val apiKey = apiKeyResult.first
    val statusCode = apiKeyResult.second

    if (apiKey == null) {
        // Handle authentication error
        callback.onFailure("API key not found or invalid (HTTP status code $statusCode)")
        return
    }

    val chatGPTAPI = getChatGPTAPI(apiKey)
    val messages = listOf(mapOf("role" to "user", "content" to prompt))

    val data = GenerateTextRequest(
        model = "gpt-3.5-turbo",
        messages = messages,
        max_tokens = 1024,
        temperature = 0.2
    )

    chatGPTAPI.generateText(data).enqueue(object : retrofit2.Callback<CompletionResponse> {
        override fun onResponse(call: Call<CompletionResponse>, response: Response<CompletionResponse>) {
            if (response.isSuccessful) {
                val text = response.body()?.choices?.getOrNull(0)?.message?.content ?: "No text found"
                callback.onSuccess(text)
            } else {
                callback.onFailure("Request failed with HTTP status code ${response.code()}: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<CompletionResponse>, t: Throwable) {
            callback.onFailure("Request failed due to a network error: ${t.message}")
        }
    })
}