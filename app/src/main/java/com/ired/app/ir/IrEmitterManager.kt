package com.ired.app.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log

class IrEmitterManager(context: Context) {

    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    // Cached once — no repeated system service calls
    val isHardwareAvailable: Boolean = irManager?.hasIrEmitter() == true

    fun getCarrierFrequencyRanges(): List<Pair<Int, Int>> {
        if (!isHardwareAvailable) return emptyList()
        return try {
            irManager?.carrierFrequencies?.map { Pair(it.minFrequency, it.maxFrequency) }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Transmit alternating burst-space microsecond timings.
     * MUST be called from a background thread — transmit() is blocking.
     */
    fun transmit(carrierHz: Int, pattern: IntArray): Result<Unit> {
        if (!isHardwareAvailable) {
            val msg = "No Consumer IR emitter detected on this device."
            Log.w(TAG, msg)
            return Result.failure(IllegalStateException(msg))
        }

        return try {
            irManager?.transmit(carrierHz, pattern)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to transmit IR signal", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "IrEmitterManager"
    }
}
