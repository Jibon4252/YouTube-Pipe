package com.example.youtubepipe

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class VideoItem(
    val url: String = "",
    val title: String = "",
    val thumbnail: String = "",
    val uploaderName: String = "",
    val views: Long = 0L,
    val duration: Long = 0L
) {
    fun videoId(): String {
        val queryId = Regex("[?&]v=([^&]+)").find(url)?.groupValues?.get(1)
        if (!queryId.isNullOrBlank()) return queryId

        val pathId = Regex("/watch/([^/?&]+)").find(url)?.groupValues?.get(1)
        if (!pathId.isNullOrBlank()) return pathId

        return url.substringAfterLast("/").substringBefore("?")
    }
}

data class SearchResponse(
    val items: List<VideoItem> = emptyList(),
    val nextpage: String? = null
)

data class StreamResponse(
    val title: String = "",
    val description: String = "",
    val uploader: String = "",
    val duration: Long = 0L,
    val views: Long = 0L,
    val likes: Long = 0L,
    val dislikes: Long = 0L,
    val videoStreams: List<VideoStream> = emptyList(),
    val audioStreams: List<AudioStream> = emptyList(),
    val relatedStreams: List<VideoItem> = emptyList(),
    val subtitles: List<SubtitleItem> = emptyList()
)

data class VideoStream(
    val url: String = "",
    val quality: String = "",
    val format: String = "",
    val mimeType: String = "",
    val videoOnly: Boolean = false
)

data class AudioStream(
    val url: String = "",
    val quality: String = "",
    val format: String = "",
    val mimeType: String = "",
    val bitrate: Int = 0
)

data class SubtitleItem(
    val url: String = "",
    val code: String = "",
    val name: String = "",
    val mimeType: String = ""
)

interface PipedApiService {
    @GET("trending")
    suspend fun getTrending(
        @Query("region") region: String = "BD"
    ): List<VideoItem>

    @GET("search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("filter") filter: String = "videos"
    ): SearchResponse

    @GET("streams/{videoId}")
    suspend fun getStreamDetails(
        @Path("videoId") videoId: String
    ): StreamResponse

    companion object {
        private const val BASE_URL = "https://pipedapi.ducks.party/"

        fun create(): PipedApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PipedApiService::class.java)
        }
    }
}
