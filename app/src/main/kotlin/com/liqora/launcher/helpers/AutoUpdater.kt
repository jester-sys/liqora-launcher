package com.liqora.launcher.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object AutoUpdater {
    private const val TAG = "AutoUpdater"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdates(context: Context, url: String, token: String, isManual: Boolean = false) {
        if (url.isBlank()) return

        DebugLogger.log(TAG, "Checking for updates: $url")

        withContext(Dispatchers.IO) {
            try {
                // 1. Parse URL to get owner/repo
                val regex = Regex("github\\.com/([^/]+)/([^/]+)")
                val match = regex.find(url)
                if (match == null) {
                    DebugLogger.log(TAG, "Invalid GitHub URL format")
                    return@withContext
                }
                val (owner, repoRaw) = match.destructured
                val repo = repoRaw.substringBefore("/").substringBefore(".git")

                DebugLogger.log(TAG, "Repo: $owner/$repo")

                // 2. Fetch latest successful workflow run
                // We filter for "status=success" and "branch=main/master" if possible
                val runsUrl = "https://api.github.com/repos/$owner/$repo/actions/runs?status=success&per_page=10"

                val runsRequest = Request.Builder()
                    .url(runsUrl)
                    .apply {
                        if (token.isNotBlank()) header("Authorization", "token $token")
                    }
                    .build()

                val runsResponse = client.newCall(runsRequest).execute()
                if (!runsResponse.isSuccessful) {
                    Log.e(TAG, "Failed to fetch runs: ${runsResponse.code} ${runsResponse.message}")
                    return@withContext
                }

                val runsBody = runsResponse.body?.string() ?: return@withContext
                val runsJson = json.parseToJsonElement(runsBody).jsonObject
                val workflowRuns = runsJson["workflow_runs"]?.jsonArray ?: return@withContext

                // Find latest run with artifacts
                var latestRun: kotlinx.serialization.json.JsonObject? = null
                var runNumber = 0

                for (i in 0 until workflowRuns.size) {
                    val run = workflowRuns[i].jsonObject
                    val hasArtifacts = run["artifacts_url"] != null
                    if (hasArtifacts) {
                        latestRun = run
                        runNumber = run["run_number"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                        break
                    }
                }

                if (latestRun == null) {
                    DebugLogger.log(TAG, "No successful runs with artifacts found")
                    return@withContext
                }

                val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                val currentBuildNumber = currentVersion?.substringAfterLast(".")?.toIntOrNull() ?: 0

                DebugLogger.log(TAG, "Version Check: Remote=$runNumber, Local=$currentBuildNumber")

                if (runNumber <= currentBuildNumber && !isManual) {
                    DebugLogger.log(TAG, "App is up to date")
                    return@withContext
                }

                val artifactsUrl = latestRun["artifacts_url"]?.jsonPrimitive?.contentOrNull ?: return@withContext
                // The run's own GitHub page — used below instead of silently
                // downloading + installing an APK, since that requires the
                // REQUEST_INSTALL_PACKAGES permission, which Google Play does
                // not allow apps to use for self-updating outside of Play's
                // own update mechanism.
                val runHtmlUrl = latestRun["html_url"]?.jsonPrimitive?.contentOrNull

                // 3. Fetch artifacts (only to confirm a build actually exists for
                // this run before telling the user an update is available)
                val artifactsRequest = Request.Builder()
                    .url(artifactsUrl)
                    .apply {
                        if (token.isNotBlank()) header("Authorization", "token $token")
                    }
                    .build()

                val artifactsResponse = client.newCall(artifactsRequest).execute()
                 if (!artifactsResponse.isSuccessful) {
                    Log.e(TAG, "Failed to fetch artifacts: ${artifactsResponse.code}")
                    return@withContext
                }

                val artifactsBody = artifactsResponse.body?.string() ?: return@withContext
                val artifactsJson = json.parseToJsonElement(artifactsBody).jsonObject
                val artifacts = artifactsJson["artifacts"]?.jsonArray
                val firstArtifact = artifacts?.firstOrNull()?.jsonObject

                if (firstArtifact == null) {
                     Log.d(TAG, "No artifacts found")
                     return@withContext
                }

                DebugLogger.log(TAG, "Update available (run #$runNumber). Opening release page for manual download.")

                // 4. Hand off to the browser instead of installing directly.
                // The app no longer downloads or installs an APK on its own.
                if (runHtmlUrl != null) {
                    withContext(Dispatchers.Main) {
                        openUpdatePage(context, runHtmlUrl)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update failed", e)
            }
        }
    }

    /** Opens the update/release page in the user's browser so they can review
     *  and, if they choose, download and sideload it themselves — the app
     *  itself never requests install-packages access or installs anything. */
    private fun openUpdatePage(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open update page", e)
        }
    }
}
