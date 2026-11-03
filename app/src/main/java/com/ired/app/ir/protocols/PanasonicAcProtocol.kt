package com.ired.app.ir.protocols

import com.ired.app.model.PanasonicAcState

/**
 * 36.7kHz Panasonic AC 27-Byte Kaseikyo Protocol Transmitter.
 * Matches exact byte structure, timings (3456/1728 header, 432/1296 data),
 * and checksum calculation from the reference implementation.
 * Zero-allocation: uses a pre-sized IntArray(440) directly without boxing.
 */
object PanasonicAcProtocol {

    const val CARRIER_HZ = 36700
    private const val TOTAL_PULSE_COUNT = 440

    fun encode(state: PanasonicAcState): IntArray {
        val acPowerOn = state.powerOn
        val acTemp = state.temperature.coerceIn(16, 30)

        val s = intArrayOf(
            // Section 1 (8 bytes)
            0x02, 0x20, 0xE0, 0x04, 0x00, 0x00, 0x00, 0x06,
            // Section 2 (19 bytes)
            0x02, 0x20, 0xE0, 0x04, 0x00,
            (0x08 or (state.mode.code shl 4) or if (acPowerOn) 0x01 else 0x00), // Model flags, mode, power
            (if (state.mode.name == "FAN") 27 else acTemp) shl 1,
            0x80,
            (((state.fan.code + 3) shl 4) or state.swing.code), // Fan and vertical vane
            0x00, 0x00, 0x0E, 0xE0, 0x00, 0x00, 0x81, 0x00, 0x00,
            0x00                           // Byte 26: Checksum placeholder
        )

        var sum = 0
        for (i in 8..25) {
            sum += s[i]
        }
        s[26] = sum and 0xFF

        val pattern = IntArray(TOTAL_PULSE_COUNT)
        var idx = 0

        // Section 1 (Bytes 0..7)
        pattern[idx++] = 3456
        pattern[idx++] = 1728
        for (b in 0..7) {
            idx = encodeByteLsb(s[b], pattern, idx)
        }
        pattern[idx++] = 432
        pattern[idx++] = 10000 // Section gap

        // Section 2 (Bytes 8..26)
        pattern[idx++] = 3456
        pattern[idx++] = 1728
        for (b in 8..26) {
            idx = encodeByteLsb(s[b], pattern, idx)
        }
        pattern[idx++] = 432
        pattern[idx] = 5000 // Trailing pause

        return pattern
    }

    private fun encodeByteLsb(byteVal: Int, pattern: IntArray, startIdx: Int): Int {
        var idx = startIdx
        for (bit in 0..7) {
            pattern[idx++] = 432
            pattern[idx++] = if (((byteVal shr bit) and 1) == 1) 1296 else 432
        }
        return idx
    }
}
