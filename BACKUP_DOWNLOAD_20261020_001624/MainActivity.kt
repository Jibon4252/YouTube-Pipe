package com.example.youtubepipe

import android.content.Intent
import android.content.ComponentName
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var pendingAudioUrl = ""
    private var pendingAudioTitle = "JR Play"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NewPipe.init(AndroidDownloader())

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        val sessionToken = SessionToken(
            this,
            ComponentName(this, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                if (pendingAudioUrl.isNotBlank()) {
                    playOnMediaController(pendingAudioUrl, pendingAudioTitle)
                }
            } catch (_: Exception) {
                mediaController = null
            }
        }, Runnable::run)

        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun playOnMediaController(url: String, title: String) {
        val controller = mediaController ?: return
        val item = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(title).build()
            )
            .build()
        controller.setMediaItem(item)
        controller.prepare()
        controller.play()
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            webView.evaluateJavascript(
                "if(window.handleAndroidBack){window.handleAndroidBack()}",
                null
            )
        }
    }

    inner class WebAppInterface(private val activity: MainActivity) {
        private val baseUrl = "https://pipedapi.ducks.party"

        @JavascriptInterface
        fun loadTrending() {
            requestInBackground("/trending?region=BD", "onTrendingResult")
        }

        @JavascriptInterface
        fun searchVideos(query: String) {
            if (query.isBlank()) {
                sendResult("onSearchResult", """{"items":[]}""")
                return
            }

            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            requestInBackground(
                "/search?q=$encoded&filter=videos",
                "onSearchResult"
            )
        }

        @JavascriptInterface
        fun getVideoDetails(videoId: String) {
            val id = videoId.trim()
            if (id.isBlank()) {
                sendResult(
                    "onVideoDetailsResult",
                    JSONObject().put("error", "Video ID is empty").toString()
                )
                return
            }

            val encoded = URLEncoder.encode(id, "UTF-8")
            requestInBackground("/streams/$encoded", "onVideoDetailsResult")
        }

        @JavascriptInterface
        fun playBackgroundAudio(url: String, title: String) {
            val safeUrl = url.trim()
            if (!safeUrl.startsWith("https://") && !safeUrl.startsWith("http://")) return
            activity.runOnUiThread {
                pendingAudioUrl = safeUrl
                pendingAudioTitle = title.ifBlank { "JR Play" }
                playOnMediaController(pendingAudioUrl, pendingAudioTitle)
            }
        }

        @JavascriptInterface
        fun exitApp() {
            activity.runOnUiThread {
                activity.finish()
            }
        }

        @JavascriptInterface
        fun openSettings() {
            activity.runOnUiThread {
                activity.startActivity(
                    Intent(activity, SettingsActivity::class.java)
                )
            }
        }

        private fun requestInBackground(path: String, callback: String) {
            Thread {
                val result = fetchApi(path)
                sendResult(callback, result)
            }.start()
        }

        private fun fetchApi(path: String): String {
            var connection: HttpURLConnection? = null

            return try {
                connection =
                    (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 15000
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("User-Agent", "YouTube-Pipe/1.0")
                    }

                val code = connection.responseCode
                val stream = if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val body = stream?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                }

                if (code in 200..299 && !body.isNullOrBlank()) {
                    body
                } else {
                    JSONObject()
                        .put("error", "Video service returned HTTP $code")
                        .toString()
                }
            } catch (_: Exception) {
                JSONObject()
                    .put(
                        "error",
                        "Unable to connect to video service. Check your internet and try again."
                    )
                    .toString()
            } finally {
                connection?.disconnect()
            }
        }

        private fun sendResult(callback: String, json: String) {
            activity.runOnUiThread {
                activity.webView.evaluateJavascript(
                    "window.$callback && window.$callback(${JSONObject.quote(json)});",
                    null
                )
            }
        }
    }
}
