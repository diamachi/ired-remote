package com.ired.app.ir.protocols

import com.ired.app.ir.NecProtocol

/**
 * Cherry Blossom Fairy Lights (13-Key 8-Mode NEC Remote).
 * Matches exact codes from your physical remote / reference PDF.
 */
object CherryLightsProtocol {

    const val CMD_ON = 0x00FFA25DL
    const val CMD_OFF = 0x00FFE21DL
    const val CMD_TIMER_6H = 0x00FF629DL

    // 8 Flash / Dynamic Modes
    const val CMD_MODE_1_COMB = 0x00FF22DDL
    const val CMD_MODE_2_WAVE = 0x00FF02FDL
    const val CMD_MODE_3_SEQ = 0x00FFC23DL
    const val CMD_MODE_4_SLOGLO = 0x00FFE01FL
    const val CMD_MODE_5_CHASE = 0x00FFA857L
    const val CMD_MODE_6_FADE = 0x00FF906FL
    const val CMD_MODE_7_TWINKLE = 0x00FF6897L
    const val CMD_MODE_8_STEADY = 0x00FFB04FL
    const val CMD_MODE_8_STEADY_ALT = 0x00FF9867L

    // Dimming
    const val CMD_DIM_MINUS = 0x00FF30CFL
    const val CMD_DIM_PLUS_A = 0x00FF7A85L
    const val CMD_DIM_PLUS_B = 0x00FF18E7L

    fun buildPattern(hexCode: Long): IntArray {
        return NecProtocol.encode32(hexCode)
    }
}
