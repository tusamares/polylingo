package com.polylingo.app.data

import com.google.gson.annotations.SerializedName
data class Sentence(
    val id: Long,
    val text: String,
    val lang: String,
    val correctness: Int,
    val script: String?,
    val license: String?,
    val translations: List<List<Translation>>?,
    val transcriptions: List<List<Transcription>>?
)

data class Translation(
    val id: Long,
    val text: String,
    val lang: String,
    val correctness: Int,
    val script: String?,
    val transcriptions: List<Transcription>
)

data class Transcription(
    val id: Long,
    val sentence_id: Long,
    val script: String?,
    val text: String,
    val user_id: Long?
)
data class SentenceResponse(
    @SerializedName("sentence") val sentence: Sentence
)

data class SentenceListResponse(
    @SerializedName("sentences") val sentences: List<Sentence>
)

data class LanguageResponse(
    @SerializedName("language") val language: Language
)