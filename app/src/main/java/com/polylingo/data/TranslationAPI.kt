package com.polylingo.app.data

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TranslationAPI {
    @GET("get")
    fun getTranslation(
        @Query("q") textToTranslate: String,
        @Query("langpair") languagePair: String
    ): Call<TranslationResponse>

    data class TranslationResponse(
        val responseData: ResponseData,
        val quotaFinished: Boolean,
        val mtLangSupported: Any,
        val responseDetails: Any,
        val responseStatus: Int
    )

    data class ResponseData(
        val translatedText: String
    )

}

