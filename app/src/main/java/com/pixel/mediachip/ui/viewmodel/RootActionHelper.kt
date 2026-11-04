package com.pixel.mediachip.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader

/**
 * Diagnostics payload representing system privileges and permissions required by the module.
 */
data class SystemDiagnostics(
    val isRootAvailable: Boolean,
    val isRecordAudioGranted: Boolean,
    val isOverlayGranted: Boolean
)

/**
 * Helper object providing asynchronous root commands and permission diagnostics.
 *
 * All process invocations execute strictly on [Dispatchers.IO] to prevent blocking the main thread.
 * Zero hardcoded colors, zero emojis.
 */
object RootActionHelper {

    private const val CMD_RESTART_SYSTEMUI = "killall com.android.systemui"
    private const val CMD_CHECK_ID = "id"

    /**
     * Executes `su -c "killall com.android.systemui"` asynchronously.
     * Returns [Result.success] when process exit code is 0, or [Result.failure] with diagnostic details.
     */
    suspend fun restartSystemUi(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("su", "-c", CMD_RESTART_SYSTEMUI)
                .redirectErrorStream(true)
                .start()
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Result.success(true)
            } else {
                val errorDetails = process.inputStream.bufferedReader().use(BufferedReader::readText)
                Result.failure(RuntimeException("Restart failed with exit code $exitCode: $errorDetails"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Checks if root access is granted by running `su -c "id"`.
     * Returns true if exit code is 0 and output contains "uid=0".
     */
    suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("su", "-c", CMD_CHECK_ID)
                .redirectErrorStream(true)
                .start()
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
                output.contains("uid=0")
            } else {
                false
            }
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Checks if [Manifest.permission.RECORD_AUDIO] has been granted to the application.
     */
    fun isRecordAudioGranted(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if [Settings.canDrawOverlays] permission is granted for floating media cards.
     */
    fun isOverlayGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Fetches current [SystemDiagnostics] status across root and permissions.
     */
    suspend fun getDiagnostics(context: Context): SystemDiagnostics = withContext(Dispatchers.IO) {
        val root = checkRootAccess()
        val audio = isRecordAudioGranted(context)
        val overlay = isOverlayGranted(context)
        SystemDiagnostics(
            isRootAvailable = root,
            isRecordAudioGranted = audio,
            isOverlayGranted = overlay
        )
    }
}
