package com.polylingo.app.data

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TatoebaAPI {

    @GET("eng/api_v0/sentence/{id}")
    fun getRandomSentence(@Path("id") id: Long): Call<Sentence>

    @GET("en/api_v0/search")
    fun searchSentences(
        @Query("from") from: String,
        @Query("sort") sort: String = "random",
        @Query("perPage") perPage: Int = 150,
        @Query("page") page: Int = 1,
        @Query("trans_filter") translationsFilter: String = "limit",
        @Query("trans_link") translationsLink: String = "direct",
        @Query("trans_to") translationsTo: String?,
        @Query("transcriptions") transcriptions: String = "has",
        @Query("query") query: String? = null  // Make query nullable
    ): Call<SearchResponse>

    data class SearchResponse(
        val paging: PagingInfo,
        val results: List<Sentence>
    )

    data class PagingInfo(
        @SerializedName("Sentences")
        val sentences: PagingMetadata
    )

    data class PagingMetadata(
        val page: Int,
        val current: Int,
        val count: Int,
        @SerializedName("perPage")
        val perPage: Int,
        val start: Int,
        val end: Int,
        val nextPage: Boolean,
        val pageCount: Int
    )

    data class Sentence(
        val id: Long,
        val text: String,
        val lang: String,
        val correctness: Int,
        val script: String?,
        val license: String?,
        val translations: List<List<Translation>> = emptyList(),
        val transcriptions: List<Transcription>  = emptyList(),
    )

    data class Translation(
        val id: Long,
        val text: String,
        val lang: String,
        val correctness: Int,
        val script: String?,
        val transcriptions: List<Transcription>,
    )

    data class Transcription(
        val id: Long,
        val sentence_id: Long,
        val script: String?,
        val text: String,
        val user_id: Long?
    )

companion object {

    val wordList = listOf(
        "唔", "冇", "睇", "嘢", "嚟", "咁", "咗", "嘅", "煲", "噉",
        "啲", "啱", "咭", "乸", "黐", "冚", "揸", "嗰", "冧", "嚫",
        "喺", "脷", "攞", "佢", "哋", "噏", "咩", "𨳊", "揦", "焗",
        "㷫", "呔", "㗎", "𨶙", "掹", "仆", "嘥", "奀", "餸", "喐",
        "踎", "膶", "嚿", "踭", "烘", "孭", "戙", "扑", "唞", "嗮",
        "抌", "揼", "繑", "𨳍", "𣲷", "𢱕", "𦧲", "𥄫", "攋", "燶",
        "腍", "响", "躝", "粿", "趷", "㨢", "揗", "睩", "㩧", "搲",
        "裇", "趯", "𡃁", "骹", "嗱", "埞", "喼", "劏", "鎅", "涌",
        "啫", "㩒", "甴", "罨", "椗", "炆", "擳", "䞘", "唎", "曱",
        "揈", "噼", "搣", "掟", "𠸎", "摙", "詏", "軚", "邨", "唥"
    )

    public val jyutwordList= listOf(
        "ngo", "nei", "keoi", "ge", "aa", "gam", "dou", "zau", "hai", "hou", "heoi", "ng", "go", "zo", "jau", "tung", "laa", "gong", "jiu", "di", "waa", "ho", "jat", "wui", "zan", "zi", "me", "ji", "soeng", "mou", "zou", "dak", "je", "sin", "gwo"
    )
    private fun getRandomCantoneseWord(): String {
        return jyutwordList.shuffled().first()
    }
    public fun getRandomCantoneseCharacter(): String {
        return wordList.shuffled().first()
    }
}
}



