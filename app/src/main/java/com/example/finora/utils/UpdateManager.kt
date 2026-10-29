package com.example.finora.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.example.finora.BuildConfig
import com.example.finora.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object UpdateManager {

    fun checkForUpdate(activity: Activity) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.checkVersion()
                if (!response.isSuccessful) return@launch

                val data = response.body() ?: return@launch
                val currentVersion = BuildConfig.VERSION_NAME

                val hasUpdate = isNewerVersion(data.latestVersion, currentVersion)
                val forceUpdate = isNewerVersion(data.minVersion, currentVersion)

                if (hasUpdate) {
                    withContext(Dispatchers.Main) {
                        showUpdateDialog(activity, data.downloadUrl, data.releaseNotes, forceUpdate)
                    }
                }
            } catch (_: Exception) {
                // No network — skip silently
            }
        }
    }

    private fun showUpdateDialog(
        activity: Activity,
        downloadUrl: String,
        releaseNotes: String,
        forceUpdate: Boolean
    ) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Update Available")
            .setMessage("A new version of Finora is available.\n\n$releaseNotes")
            .setPositiveButton("Download") { _, _ ->
                // Open browser to download APK
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                activity.startActivity(intent)
            }
            .setCancelable(!forceUpdate) // force update = can't dismiss

        if (!forceUpdate) {
            dialog.setNegativeButton("Later", null)
        }

        dialog.show()
    }

    /** Returns true if versionA is newer than versionB */
    private fun isNewerVersion(versionA: String, versionB: String): Boolean {
        val a = versionA.split(".").map { it.toIntOrNull() ?: 0 }
        val b = versionB.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(a.size, b.size)
        for (i in 0 until maxLen) {
            val partA = a.getOrElse(i) { 0 }
            val partB = b.getOrElse(i) { 0 }
            if (partA > partB) return true
            if (partA < partB) return false
        }
        return false
    }
}