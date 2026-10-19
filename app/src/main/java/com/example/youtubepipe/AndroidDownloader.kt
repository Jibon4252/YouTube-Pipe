package com.example.youtubepipe

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.net.HttpURLConnection
import java.net.URL

class AndroidDownloader : Downloader() {

    override fun execute(request: Request): Response {
        var connection: HttpURLConnection? = null

        try {
            connection = (URL(request.url()).openConnection() as HttpURLConnection).apply {
                requestMethod = request.httpMethod()
                connectTimeout = 15000
                readTimeout = 20000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "YouTube-Pipe/1.0")

                request.headers().forEach { (name, values) ->
                    values.forEachIndexed { index, value ->
                        if (index == 0) setRequestProperty(name, value)
                        else addRequestProperty(name, value)
                    }
                }

                request.dataToSend()?.let { data ->
                    doOutput = true
                    outputStream.use { it.write(data) }
                }
            }

            val code = connection.responseCode
            val body = (
                if (code in 200..299) connection.inputStream
                else connection.errorStream
            )?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

            val headers: Map<String, List<String>> =
                connection.headerFields
                    .filterKeys { it != null }
                    .mapKeys { it.key!! }
                    .mapValues { it.value ?: emptyList() }

            return Response(
                code,
                connection.responseMessage ?: "",
                headers,
                body,
                connection.url.toString()
            )
        } finally {
            connection?.disconnect()
        }
    }
}
