package com.polylingo.app.data

import com.google.gson.annotations.SerializedName

data class Language(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String
)

data class LanguageListResponse(
    @SerializedName("languages") val languages: List<Language>
)