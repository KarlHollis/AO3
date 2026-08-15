package com.ao3.app.upload

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.ao3.app.viewmodel.FileChooserRequest
import java.io.File

class FileUploadManager(private val activity: ComponentActivity) {

    companion object {
        private const val MAX_PICK_IMAGES = 10
        private val IMAGE_EXTENSIONS = setOf(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".heic", ".heif",
        )
    }

    private var pendingCallback: ValueCallback<Array<Uri>>? = null
    private var pendingAllowMultiple: Boolean = false
    private var pendingCaptureUri: Uri? = null
    private var currentDialog: AlertDialog? = null

    private val cameraLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = pendingCallback
        val captureUri = pendingCaptureUri
        pendingCallback = null
        pendingCaptureUri = null
        pendingAllowMultiple = false
        if (result.resultCode == Activity.RESULT_OK && captureUri != null) {
            callback?.onReceiveValue(arrayOf(captureUri))
        } else {
            deleteCaptureFile(captureUri)
            callback?.onReceiveValue(null)
        }
    }

    private val photoPickerLauncher = activity.registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK_IMAGES)
    ) { uris -> completeWith(uris) }

    private val documentLauncher = activity.registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> completeWith(uris) }

    fun handleRequest(request: FileChooserRequest) {
        currentDialog?.dismiss()
        currentDialog = null
        pendingCallback?.onReceiveValue(null)
        pendingCallback = request.callback
        pendingAllowMultiple =
            request.params.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE

        val types = request.params.acceptTypes
        val acceptsImages = types.isEmpty() || types.all { isImageType(it) }
        when {
            request.params.isCaptureEnabled() -> launchCamera()
            acceptsImages -> showImageSourceDialog(includeFiles = types.isEmpty())
            else -> launchDocumentPicker(types)
        }
    }

    private fun showImageSourceDialog(includeFiles: Boolean) {
        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()
        options += "相册"
        actions += ::launchGallery
        options += "拍照"
        actions += ::launchCamera
        if (includeFiles) {
            options += "其他文件"
            actions += { launchDocumentPicker(emptyArray()) }
        }
        currentDialog = AlertDialog.Builder(activity)
            .setTitle("选择图片来源")
            .setItems(options.toTypedArray()) { _, which -> actions[which]() }
            .setNegativeButton("取消") { _, _ -> cancelPending() }
            .setOnCancelListener { cancelPending() }
            .show()
    }

    private fun launchGallery() {
        // Android 13+ 使用系统照片选择器（无需任何权限），旧版本自动回退到 ACTION_OPEN_DOCUMENT
        photoPickerLauncher.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    private fun launchCamera() {
        val captureUri = createCaptureUri()
        if (captureUri == null) {
            cancelPending()
            return
        }
        pendingCaptureUri = captureUri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        try {
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            pendingCaptureUri = null
            cancelPending()
        }
    }

    private fun launchDocumentPicker(types: Array<String>) {
        val mimeTypes = types.mapNotNull { toMimeType(it) }.distinct().toTypedArray()
        documentLauncher.launch(if (mimeTypes.isEmpty()) arrayOf("*/*") else mimeTypes)
    }

    private fun completeWith(uris: List<Uri>) {
        val callback = pendingCallback ?: return
        val allowMultiple = pendingAllowMultiple
        pendingCallback = null
        pendingAllowMultiple = false
        pendingCaptureUri = null
        val selected = uris.filter { it != Uri.EMPTY }
        when {
            selected.isEmpty() -> callback.onReceiveValue(null)
            allowMultiple -> callback.onReceiveValue(selected.toTypedArray())
            else -> callback.onReceiveValue(arrayOf(selected.first()))
        }
    }

    private fun cancelPending() {
        pendingCallback?.onReceiveValue(null)
        pendingCallback = null
        pendingAllowMultiple = false
        pendingCaptureUri = null
    }

    private fun createCaptureUri(): Uri? {
        return try {
            val directory = File(activity.cacheDir, "camera")
            if (!directory.exists() && !directory.mkdirs()) return null
            val file = File.createTempFile("capture_", ".jpg", directory)
            FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    private fun deleteCaptureFile(uri: Uri?) {
        if (uri == null) return
        try {
            File(uri.path ?: return).delete()
        } catch (e: Exception) {
            // 删除失败不影响功能，缓存目录由系统统一回收
        }
    }

    private fun isImageType(type: String): Boolean {
        val normalized = type.trim().lowercase()
        return normalized.startsWith("image/") || normalized in IMAGE_EXTENSIONS
    }

    private fun toMimeType(type: String): String? {
        val normalized = type.trim().lowercase()
        if (normalized.isBlank()) return null
        if (normalized.startsWith(".")) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(normalized.removePrefix("."))
        }
        return normalized
    }
}
