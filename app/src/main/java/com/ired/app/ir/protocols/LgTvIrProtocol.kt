package com.ired.app.ir.protocols

import com.ired.app.ir.NecProtocol

/**
 * LG webOS TV IR Protocol (38kHz NEC, Address 0x20DF).
 * Pure LG webOS television control.
 */
object LgTvIrProtocol {

    const val CMD_POWER = 0x20DF10EFL          // TV Power Toggle
    const val CMD_INPUT = 0x20DFD02FL          // Input Source Select
    const val CMD_SETTINGS = 0x20DFC23DL       // LG webOS All Settings / Picture / Audio
    const val CMD_Q_MENU = 0x20DFA25DL         // LG Quick Setup Menu
    const val CMD_HOME = 0x20DF3EC1L           // LG webOS Home Dashboard
    const val CMD_INFO = 0x20DF55AAL           // Channel / Source Info
    const val CMD_BACK = 0x20DF14EBL           // Back / Return
    const val CMD_EXIT = 0x20DFDA25L           // Exit All Menus

    // Directional Navigation
    const val CMD_UP = 0x20DF02FDL
    const val CMD_DOWN = 0x20DF827DL
    const val CMD_LEFT = 0x20DFE01FL
    const val CMD_RIGHT = 0x20DF609FL
    const val CMD_OK = 0x20DF22DDL

    // Volume & Audio
    const val CMD_VOL_UP = 0x20DF40BFL
    const val CMD_VOL_DOWN = 0x20DFC03FL
    const val CMD_MUTE = 0x20DF906FL

    // TV Channels
    const val CMD_CH_UP = 0x20DF00FFL
    const val CMD_CH_DOWN = 0x20DF807FL

    // Voice / Search
    const val CMD_ASSISTANT = 0x20DF4EB1L

    // Media Playback
    const val CMD_PLAY = 0x20DF0DF2L
    const val CMD_PAUSE = 0x20DF5DA2L
    const val CMD_REWIND = 0x20DFE11EL
    const val CMD_FAST_FWD = 0x20DF619EL

    fun buildPattern(hexCode: Long): IntArray {
        return NecProtocol.encode32(hexCode)
    }
}
