package com.henryai.aviationwatch.enginetime

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Uploads stored engine-time flights to the Enginetime Google Drive folder as soon as the
 * watch has a network connection (Wi-Fi, LTE or the phone's Bluetooth link). WorkManager
 * persists the job across app restarts and reboots and retries with backoff.
 */
object EnginetimeSync {
    private const val WORK_NAME = "enginetime-sync"

    fun schedule(context: Context) {
        val request = OneTimeWorkRequestBuilder<EnginetimeSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}

class EnginetimeSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = EngineLogStore.get(applicationContext)
        val files = store.pendingFiles()
        // Nothing to send, or no account yet: flights stay on the watch; signing in schedules a new sync.
        if (files.isEmpty() || !EnginetimeAuth.isConfigured || EnginetimeAuth.signedInEmail(applicationContext) == null) {
            return Result.success()
        }
        return try {
            val token = EnginetimeAuth.accessToken(applicationContext) ?: return Result.success()
            for (file in files) {
                if (!DriveClient.exists(token, file.name)) {
                    DriveClient.upload(token, file.name, file.readText())
                }
                store.markSent(file)
            }
            Result.success()
        } catch (e: EnginetimeAuth.SignInRequiredException) {
            Result.success()
        } catch (e: DriveClient.UnauthorizedException) {
            EnginetimeAuth.invalidateAccessToken()
            Result.retry()
        } catch (e: IOException) {
            Result.retry()
        }
    }
}
