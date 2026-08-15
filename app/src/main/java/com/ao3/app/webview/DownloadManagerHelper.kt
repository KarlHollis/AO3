package com.ao3.app.webview

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebSettings
import com.ao3.app.viewmodel.DownloadRequest
import java.util.concurrent.ConcurrentHashMap

object DownloadManagerHelper {

    private const val FALLBACK_FILE_NAME = "download"

    private val trackedDownloadIds = ConcurrentHashMap.newKeySet<Long>()

    fun enqueue(context: Context, request: DownloadRequest): Long {
        val fileName = guessFileName(request)
        val uri = Uri.parse(request.url)
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw IllegalArgumentException("不支持的下载地址：$scheme")
        }
        val downloadRequest = DownloadManager.Request(uri).apply {
            setMimeType(request.mimeType ?: "application/octet-stream")
            addRequestHeader("User-Agent", WebSettings.getDefaultUserAgent(context))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setTitle(fileName)
            setDescription("AO3 文件下载")
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(downloadRequest)
        trackedDownloadIds += downloadId
        return downloadId
    }

    fun guessFileName(request: DownloadRequest): String {
        val guessed = URLUtil.guessFileName(request.url, request.contentDisposition, request.mimeType)
            ?.trim()
            .orEmpty()
        val candidate = if (guessed.isBlank() || guessed.equals(FALLBACK_FILE_NAME, ignoreCase = true)) {
            fallbackName(request)
        } else {
            guessed
        }
        return sanitizeFileName(ensureExtension(candidate, request.mimeType))
    }

    fun handleDownloadComplete(context: Context, downloadId: Long): String? {
        if (!trackedDownloadIds.remove(downloadId)) return null
        val fileName = queryFileName(context, downloadId) ?: "文件"
        return when (queryStatus(context, downloadId)) {
            DownloadManager.STATUS_SUCCESSFUL -> "下载完成：$fileName"
            DownloadManager.STATUS_FAILED -> "下载失败：$fileName"
            else -> null
        }
    }

    private fun fallbackName(request: DownloadRequest): String {
        val pathSegment = Uri.parse(request.url).path?.substringAfterLast('/')
        return pathSegment
            ?.takeIf { it.isNotBlank() && it != "/" }
            ?: FALLBACK_FILE_NAME
    }

    private fun ensureExtension(fileName: String, mimeType: String?): String {
        if (fileName.contains('.')) return fileName
        val extension = extensionForMimeType(mimeType)
        return if (extension != null) "$fileName.$extension" else fileName
    }

    private fun extensionForMimeType(mimeType: String?): String? {
        if (mimeType.isNullOrBlank()) return null
        val normalized = mimeType.substringBefore(';').trim().lowercase()
        return when (normalized) {
            "application/epub+zip" -> "epub"
            "application/pdf" -> "pdf"
            "text/html" -> "html"
            else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(normalized)
        }
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("""[/\\:*?"<>|]"""), "_").trim()
        return cleaned.ifBlank { FALLBACK_FILE_NAME }
    }

    private fun queryStatus(context: Context, downloadId: Long): Int? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (index >= 0) return cursor.getInt(index)
            }
        }
        return null
    }

    private fun queryFileName(context: Context, downloadId: Long): String? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                if (index >= 0) return cursor.getString(index)
            }
        }
        return null
    }
}
