package com.ired.app.ir

/**
 * 38kHz NEC Protocol transmitter for Cherry Blossom LEDs, LG TV, and Google TV Streamer.
 * Matches exact 32-bit MSB-first timings from the reference hardware implementation.
 * Zero-allocation: uses a pre-sized IntArray(68) directly without boxing.
 */
object NecProtocol {

    const val CARRIER_HZ = 38000

    fun encode32(hexCode: Long): IntArray {
        val pattern = IntArray(68)
        var idx = 0

        // NEC Header
        pattern[idx++] = 9000
        pattern[idx++] = 4500

        // 32 bits (MSB first)
        for (i in 31 downTo 0) {
            pattern[idx++] = 560
            val bit = (hexCode shr i) and 1L
            pattern[idx++] = if (bit == 1L) 1690 else 560
        }

        // Stop bit + trailing gap
        pattern[idx++] = 560
        pattern[idx] = 20000

        return pattern
    }
}
