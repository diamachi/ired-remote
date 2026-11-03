package com.ired.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ired.app.data.AppPreferences
import com.ired.app.data.RemoteRepository
import com.ired.app.data.StatusEvent
import com.ired.app.gtv.GtvDiscoveredDevice
import com.ired.app.gtv.GtvManager
import com.ired.app.gtv.GtvState
import com.ired.app.model.CustomButton
import com.ired.app.model.PanasonicAcState
import com.ired.app.model.PanasonicMode
import com.ired.app.model.PanasonicFan
import com.ired.app.model.PanasonicSwing
import com.ired.app.model.UserRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)
    private val repository = RemoteRepository(application, preferences)

    val remotes: StateFlow<List<UserRemote>> = repository.remotes

    // Expose only what UI needs — no leaking repository internals
    val acState: StateFlow<PanasonicAcState> = repository.acState
    val statusEvents: SharedFlow<StatusEvent> = repository.statusEvents

    // IR hardware info — cached once, never recomputed
    val isIrHardwareAvailable: Boolean = repository.irManager.isHardwareAvailable
    val irCarrierRanges: List<Pair<Int, Int>> = repository.irManager.getCarrierFrequencyRanges()

    // Each Google TV remote owns its own connection, discovery state, and saved profile.
    private val gtvManagers = java.util.concurrent.ConcurrentHashMap<String, GtvManager>()
    private fun gtvManager(remoteId: String): GtvManager = gtvManagers.computeIfAbsent(remoteId) {
        GtvManager(getApplication(), preferences, it)
    }
    fun gtvState(remoteId: String): StateFlow<GtvState> = gtvManager(remoteId).state
    fun gtvDiscoveredDevices(remoteId: String): StateFlow<List<GtvDiscoveredDevice>> = gtvManager(remoteId).discoveredDevices
    fun startGtvDiscovery(remoteId: String) = gtvManager(remoteId).startDiscovery()
    fun stopGtvDiscovery(remoteId: String) = gtvManagers[remoteId]?.stopDiscovery()
    fun pairGtv(remoteId: String, host: String, name: String) = gtvManager(remoteId).startPairing(host, name)
    fun submitGtvPin(remoteId: String, pin: String) = gtvManager(remoteId).submitPairingPin(pin)
    fun connectGtv(remoteId: String, host: String, name: String) = gtvManager(remoteId).connect(host, name)
    fun cancelGtvConnection(remoteId: String) = gtvManager(remoteId).cancelConnection()
    fun disconnectGtv(remoteId: String) = gtvManager(remoteId).disconnect()
    fun unpairGtv(remoteId: String) = gtvManager(remoteId).unpair()
    fun sendGtvKey(remoteId: String, keyCode: Int) = gtvManager(remoteId).sendKey(keyCode)
    fun usesLgIrVolume(remoteId: String) = preferences.getGoogleTvUsesLgIrVolume(remoteId)
    fun setUsesLgIrVolume(remoteId: String, enabled: Boolean) = preferences.setGoogleTvUsesLgIrVolume(remoteId, enabled)

    override fun onCleared() {
        gtvManagers.values.forEach { it.close() }
        gtvManagers.clear()
        super.onCleared()
    }

    private val _selectedRemoteId = MutableStateFlow<String>(
        remotes.value.firstOrNull()?.id ?: "settings"
    )
    val selectedRemoteId: StateFlow<String> = _selectedRemoteId.asStateFlow()

    fun selectRemote(remoteId: String) {
        _selectedRemoteId.value = remoteId
    }

    fun addRemote(remote: UserRemote) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addRemote(remote)
        }
        _selectedRemoteId.value = remote.id
    }

    fun renameRemote(remoteId: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.renameRemote(remoteId, name) }
    }

    fun removeRemote(remoteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeRemote(remoteId)
        }
        gtvManagers.remove(remoteId)?.close()
        preferences.clearGoogleTvProfile(remoteId)
        val remaining = remotes.value.filterNot { it.id == remoteId }
        _selectedRemoteId.value = remaining.firstOrNull()?.id ?: "settings"
    }

    fun addCustomButton(remoteId: String, button: CustomButton) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addCustomButtonToRemote(remoteId, button)
        }
    }

    fun removeCustomButton(remoteId: String, buttonId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeCustomButtonFromRemote(remoteId, buttonId)
        }
    }

    // IR commands — all dispatched to background thread
    fun sendNecCode(hexCode: Long, label: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.sendNec(hexCode, label)
        }
    }

    fun sendCherryCode(hexCode: Long, label: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.sendNec(hexCode, label)
        }
    }

    fun setAcTemp(temp: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setAcTemp(temp)
        }
    }

    fun updateAcMode(mode: PanasonicMode) = viewModelScope.launch(Dispatchers.IO) { repository.updateAcMode(mode) }
    fun updateAcFan(fan: PanasonicFan) = viewModelScope.launch(Dispatchers.IO) { repository.updateAcFan(fan) }
    fun updateAcSwing(swing: PanasonicSwing) = viewModelScope.launch(Dispatchers.IO) { repository.updateAcSwing(swing) }

    fun setAcDirect(temp: Int, powerOn: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setAcDirect(temp, powerOn)
        }
    }

    fun testIrHardware() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.sendNec(0x20DF10EFL, "Test 38kHz Pulse")
        }
    }
}
