package com.dizop.print.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionCode: Int,
    val latestVersionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val expectedSha256: String = ""
)

class InAppUpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow<UpdateInfo?>(null)
    val updateState: StateFlow<UpdateInfo?> = _updateState.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    /**
     * Checks for updates against a remote JSON manifest
     */
    suspend fun checkForUpdates(manifestUrl: String = "https://raw.githubusercontent.com/parakhmewara2002-gif/Dizop/main/version.json"): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }

            val url = URL(manifestUrl)
            Log.d("InAppUpdate", "Checking update at $manifestUrl (Current code: $currentVersionCode)")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "Dizop-Android-App")
            }
            val responseCode = connection.responseCode
            Log.d("InAppUpdate", "Manifest HTTP response code: $responseCode")
            if (responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("InAppUpdate", "Manifest content: $jsonStr")
                val json = JSONObject(jsonStr)
                val remoteCode = json.optInt("versionCode", 0)
                val remoteName = json.optString("versionName", "1.0.0")
                val notes = json.optString("changelog", "Performance improvements & bug fixes")
                val apkUrl = json.optString("apkUrl", "")
                val sha = json.optString("expectedSha256", "")

                val update = UpdateInfo(
                    hasUpdate = remoteCode > currentVersionCode,
                    latestVersionCode = remoteCode,
                    latestVersionName = remoteName,
                    releaseNotes = notes,
                    downloadUrl = apkUrl,
                    expectedSha256 = sha
                )
                Log.d("InAppUpdate", "Parsed update: hasUpdate=${update.hasUpdate}, remoteCode=$remoteCode vs local=$currentVersionCode")
                _updateState.value = update
                return@withContext update
            }
        } catch (e: Exception) {
            Log.e("InAppUpdate", "Error checking for updates", e)
        }
        null
    }

    /**
     * Downloads and launches APK package installer with SHA-256 integrity verification
     */
    fun startUpdateDownload(apkUrl: String, expectedSha256: String = "") {
        if (_isDownloading.value) return
        _isDownloading.value = true

        val fileName = "DizopUpdate.apk"
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Dizop Update")
            .setDescription("Downloading latest version securely...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    _isDownloading.value = false
                    context.unregisterReceiver(this)

                    // Verify APK SHA-256 checksum if provided
                    if (expectedSha256.isNotBlank()) {
                        val computed = calculateSha256(destination)
                        if (!computed.equals(expectedSha256, ignoreCase = true)) {
                            Log.e("InAppUpdate", "Integrity check failed: $computed != $expectedSha256")
                            destination.delete()
                            return
                        }
                    }

                    installDownloadedApk(destination)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun calculateSha256(file: File): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    private fun installDownloadedApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
