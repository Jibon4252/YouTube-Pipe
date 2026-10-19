package com.example.youtubepipe

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class VideoItem(
    val url: String,
    val title: String,
    val thumbnail: String,
    val uploaderName: String,
    val views: Long,
    val duration: Long
)

data class TrendingResponse(
    val items: List<VideoItem>
)

data class StreamResponse(
    val videoStreams: List<VideoStream>
)

data class VideoStream(
    val url: String,
    val quality: String,
    val format: String,
    val videoOnly: Boolean
)

interface PipedApiService {
    @GET("trending")
    suspend fun getTrending(@Query("region") region: String = "US"): List<VideoItem>

    @GET("streams/{videoId}")
    suspend fun getStreamDetails(@Path("videoId") videoId: String): StreamResponse

    companion object {
        private const val BASE_URL = "https://pipedapi.kavin.rocks/"

        fun create(): PipedApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PipedApiService::class.java)
        }
    }
}
