package com.ired.app.model

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class RemoteType(val defaultTitle: String, val category: String) {
    LG_TV("LG webOS TV", "Television (IR)"),
    GOOGLE_TV("Google TV Streamer", "Streaming Device (BT / Wi-Fi)"),
    PANASONIC_AC("Panasonic AC", "Air Conditioner (IR)"),
    CHERRY_LIGHTS("Cherry Blossom Lights", "13-Key Fairy Lights (IR)"),
    CUSTOM_REMOTE("Custom Remote", "IR Appliance")
}

@Immutable
data class CustomButton(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val hexCode: Long, // 32-bit NEC hex code e.g. 0x20DF10EFL
    val colorHex: String = "#1E293B"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("label", label)
        put("hexCode", hexCode)
        put("colorHex", colorHex)
    }

    companion object {
        fun fromJson(json: JSONObject): CustomButton {
            return CustomButton(
                id = json.optString("id", UUID.randomUUID().toString()),
                label = json.optString("label", "Button"),
                hexCode = json.optLong("hexCode", 0L),
                colorHex = json.optString("colorHex", "#1E293B")
            )
        }
    }
}

@Immutable
data class UserRemote(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: RemoteType,
    val customButtons: List<CustomButton> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type.name)
        val arr = JSONArray()
        customButtons.forEach { arr.put(it.toJson()) }
        put("customButtons", arr)
    }

    companion object {
        fun fromJson(json: JSONObject): UserRemote {
            val typeStr = json.optString("type", RemoteType.CUSTOM_REMOTE.name)
            val type = try { RemoteType.valueOf(typeStr) } catch (_: Exception) { RemoteType.CUSTOM_REMOTE }

            val btns = mutableListOf<CustomButton>()
            val arr = json.optJSONArray("customButtons")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) btns.add(CustomButton.fromJson(obj))
                }
            }

            return UserRemote(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", type.defaultTitle),
                type = type,
                customButtons = btns
            )
        }
    }
}

@Immutable
enum class PanasonicMode(val label: String, val code: Int) {
    AUTO("Auto", 0), DRY("Dry", 2), COOL("Cool", 3), HEAT("Heat", 4), FAN("Fan", 6)
}

enum class PanasonicFan(val label: String, val code: Int) {
    AUTO("Auto", 7), LOW("Low", 1), MEDIUM("Med", 2), HIGH("High", 3), MAX("Max", 4)
}

enum class PanasonicSwing(val label: String, val code: Int) {
    AUTO("Auto", 15), HIGHEST("High", 1), MIDDLE("Middle", 3), LOWEST("Low", 5)
}

data class PanasonicAcState(
    val powerOn: Boolean = true,
    val temperature: Int = 24,
    val mode: PanasonicMode = PanasonicMode.COOL,
    val fan: PanasonicFan = PanasonicFan.AUTO,
    val swing: PanasonicSwing = PanasonicSwing.AUTO
)

@Immutable
data class CherryLightsState(
    val powerOn: Boolean = true,
    val activeMode: String = "8: Steady"
)
