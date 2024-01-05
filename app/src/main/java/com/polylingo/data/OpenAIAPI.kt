package com.polylingo.data

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody
import java.time.LocalDateTime
import java.util.Date


interface OpenAIApi {
    @Headers("Authorization: Bearer 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")
    @POST("v1/chat/completions")
    @Streaming
    fun textCompletionsWithStream(@Body body: RequestBody): Call<ResponseBody>
}

data class TextCompletionsParam(
    @SerializedName("prompt")
    val promptText: String = "",
    @SerializedName("temperature")
    val temperature: Double = 0.9,
    @SerializedName("top_p")
    val topP: Double = 1.0,
    @SerializedName("n")
    val n: Int = 1,
    @SerializedName("stream")
    var stream: Boolean = false,
    @SerializedName("maxTokens")
    val maxTokens: Int = 2048,
    @SerializedName("model")
    val model: GPTModel = GPTModel.gpt35Turbo,
    @SerializedName("messages")
    val messagesTurbo: List<MessageTurbo> = emptyList(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextCompletionsParam

        if (promptText != other.promptText) return false
        if (temperature != other.temperature) return false
        if (topP != other.topP) return false
        if (n != other.n) return false
        if (stream != other.stream) return false
        if (maxTokens != other.maxTokens) return false
        if (model != other.model) return false
        if (messagesTurbo != other.messagesTurbo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = promptText.hashCode()
        result = 31 * result + temperature.hashCode()
        result = 31 * result + topP.hashCode()
        result = 31 * result + n
        result = 31 * result + stream.hashCode()
        result = 31 * result + maxTokens
        result = 31 * result + model.hashCode()
        result = 31 * result + messagesTurbo.hashCode()
        return result
    }
}

fun TextCompletionsParam.toJson(): JsonObject {
    val json = JsonObject()
    json.addProperty("temperature", temperature)
    json.addProperty("stream", stream)
    json.addProperty("model", model.model)

    if (model == GPTModel.gpt35Turbo) {
        val jsonArray = JsonArray()
        for (message in messagesTurbo) jsonArray.add(message.toJson())

        json.add("messages", jsonArray)
    } else {
        json.addProperty("prompt", promptText)
    }

    return json
}
enum class GPTModel(val model: String, val maxTokens: Int) {
    gpt35Turbo("gpt-3.5-turbo", 4000),
    davinci("text-davinci-003", 4000),
    curie("text-curie-001", 2048),
    babbage("text-babbage-001", 2048),
    ada("text-ada-001", 2048)
}

data class MessageTurbo(
    @SerializedName("content")
    val content: String = "",
    @SerializedName("role")
    val role: TurboRole = TurboRole.user,
)

fun MessageTurbo.toJson() : JsonObject {
    val json = JsonObject()
    json.addProperty("content", content)
    json.addProperty("role", role.value)

    return json
}


enum class TurboRole(val value: String) {
    @SerializedName("system")
    system("system"),
    @SerializedName("assistant")
    assistant("assistant"),
    @SerializedName("user")
    user("user")
}
