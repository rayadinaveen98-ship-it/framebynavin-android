package com.framebynavin.app.data

import android.os.Build
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue

internal object HardeningTestEnvironment {
    fun requireCiEmulator() {
        val args = InstrumentationRegistry.getArguments()
        val hardware = Build.HARDWARE.lowercase()
        assertTrue("Destructive persistence tests require the explicit CI-emulator marker and emulator hardware.",
            args.getString("hardeningTestEnvironment") == "ci-emulator" &&
                (hardware == "ranchu" || hardware == "goldfish"))
    }

    suspend fun <T> stage(name: String, block: suspend () -> T): T {
        Log.i("FBN-Hardening", "START $name")
        return try {
            withTimeout(25_000) { block() }.also { Log.i("FBN-Hardening", "PASS $name") }
        } catch (error: Throwable) {
            Log.e("FBN-Hardening", "FAILED $name", error)
            if (error is kotlinx.coroutines.TimeoutCancellationException) {
                Thread.getAllStackTraces().forEach { (thread, trace) ->
                    Log.e("FBN-Hardening", "Thread ${thread.name}:\n${trace.joinToString("\n")}")
                }
            }
            throw error
        }
    }
}
