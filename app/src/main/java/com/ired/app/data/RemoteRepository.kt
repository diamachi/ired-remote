package com.ired.app.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ired.app.ir.IrEmitterManager
import com.ired.app.ir.NecProtocol
import com.ired.app.ir.protocols.CherryLightsProtocol
import com.ired.app.ir.protocols.LgTvIrProtocol
import com.ired.app.ir.protocols.PanasonicAcProtocol
import com.ired.app.model.CustomButton
import com.ired.app.model.PanasonicAcState
import com.ired.app.model.PanasonicMode
import com.ired.app.model.PanasonicFan
import com.ired.app.model.PanasonicSwing
import com.ired.app.model.UserRemote
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class StatusEvent(val id: Long = System.currentTimeMillis(), val message: String)

class RemoteRepository(
    private val context: Context,
    val preferences: AppPreferences
) {

    val irManager = IrEmitterManager(context)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val _remotes = MutableStateFlow<List<UserRemote>>(preferences.loadRemotes())
    val remotes: StateFlow<List<UserRemote>> = _remotes.asStateFlow()

    private val _acState = MutableStateFlow(PanasonicAcState())
    val acState: StateFlow<PanasonicAcState> = _acState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _statusEvents = MutableSharedFlow<StatusEvent>(extraBufferCapacity = 64)
    val statusEvents: SharedFlow<StatusEvent> = _statusEvents.asSharedFlow()

    fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    private fun postStatus(msg: String) {
        _statusMessage.value = msg
        _statusEvents.tryEmit(StatusEvent(message = msg))
    }

    // Remote Management
    @Synchronized fun addRemote(remote: UserRemote) {
        val current = _remotes.value.toMutableList()
        current.add(remote)
        _remotes.value = current
        preferences.saveRemotes(current)
        postStatus("Added remote: ${remote.name}")
    }

    @Synchronized fun renameRemote(remoteId: String, name: String) {
        val normalized = name.trim().take(40)
        if (normalized.isEmpty()) return
        val updated = _remotes.value.map { if (it.id == remoteId) it.copy(name = normalized) else it }
        _remotes.value = updated
        preferences.saveRemotes(updated)
        postStatus("Renamed remote to $normalized")
    }

    @Synchronized fun removeRemote(remoteId: String) {
        val current = _remotes.value.toMutableList()
        val removed = current.find { it.id == remoteId }
        current.removeAll { it.id == remoteId }
        _remotes.value = current
        preferences.saveRemotes(current)
        postStatus("Removed: ${removed?.name ?: "Remote"}")
    }

    @Synchronized fun addCustomButtonToRemote(remoteId: String, button: CustomButton) {
        val current = _remotes.value.map { remote ->
            if (remote.id == remoteId) {
                remote.copy(customButtons = remote.customButtons + button)
            } else {
                remote
            }
        }
        _remotes.value = current
        preferences.saveRemotes(current)
        postStatus("Added button: ${button.label}")
    }

    @Synchronized fun removeCustomButtonFromRemote(remoteId: String, buttonId: String) {
        val current = _remotes.value.map { remote ->
            if (remote.id == remoteId) {
                remote.copy(customButtons = remote.customButtons.filterNot { it.id == buttonId })
            } else {
                remote
            }
        }
        _remotes.value = current
        preferences.saveRemotes(current)
    }

    // --- NEC 38kHz Transmitter ---
    @Synchronized fun sendNec(hexCode: Long, label: String) {
        triggerHaptic()
        val pattern = NecProtocol.encode32(hexCode)
        val result = irManager.transmit(38000, pattern)
        result.onSuccess {
            postStatus("Sent: $label (0x${hexCode.toString(16).uppercase()})")
        }.onFailure {
            postStatus("IR Error: ${it.message}")
        }
    }

    // --- Panasonic AC 36.7kHz Transmitter ---
    @Synchronized fun sendPanasonicAc(state: PanasonicAcState) {
        triggerHaptic()
        _acState.value = state
        val pattern = PanasonicAcProtocol.encode(state)
        val result = irManager.transmit(36700, pattern)
        result.onSuccess {
            postStatus("AC command sent: ${if (state.powerOn) "ON" else "OFF"} | ${state.temperature}°C. Device state is unconfirmed.")
        }.onFailure {
            postStatus("IR Error: ${it.message}")
        }
    }

    fun setAcTemp(temp: Int) {
        val clamped = temp.coerceIn(16, 30)
        sendPanasonicAc(_acState.value.copy(temperature = clamped, powerOn = true))
    }

    fun toggleAcPower() {
        sendPanasonicAc(_acState.value.copy(powerOn = !_acState.value.powerOn))
    }

    fun setAcDirect(temp: Int, powerOn: Boolean = true) {
        sendPanasonicAc(_acState.value.copy(temperature = temp.coerceIn(16, 30), powerOn = powerOn))
    }

    fun updateAcMode(mode: PanasonicMode) = sendPanasonicAc(_acState.value.copy(mode = mode, powerOn = true))
    fun updateAcFan(fan: PanasonicFan) = sendPanasonicAc(_acState.value.copy(fan = fan, powerOn = true))
    fun updateAcSwing(swing: PanasonicSwing) = sendPanasonicAc(_acState.value.copy(swing = swing, powerOn = true))
}
