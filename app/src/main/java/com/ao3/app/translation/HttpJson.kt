package com.ao3.app.translation

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object HttpJson {

    fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        contentType: String = "application/json",
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 90_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", contentType)
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IOException("HTTP $code: ${text.take(300)}")
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    fun encodeForm(params: Map<String, String>): String =
        params.entries.joinToString("&") { (key, value) ->
            URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
        }
}
