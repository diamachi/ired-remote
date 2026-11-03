package com.ired.app.gtv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.ired.app.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.security.cert.X509Certificate
import java.security.MessageDigest
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket

private const val TAG = "GtvManager"
private const val PORT_PAIRING = 6467
private const val PORT_COMMAND = 6466
private const val SERVICE_TYPE = "_androidtvremote2._tcp"

data class GtvDiscoveredDevice(
    val name: String,
    val host: String,
    val port: Int = PORT_COMMAND
)

sealed class GtvState {
    data object Disconnected : GtvState()
    data class Discovering(val devices: List<GtvDiscoveredDevice> = emptyList()) : GtvState()
    data class PairingPrompt(val host: String, val tvName: String) : GtvState()
    data class Connecting(val host: String) : GtvState()
    data class Connected(val host: String, val tvName: String) : GtvState()
    data class Error(val message: String, val host: String? = null) : GtvState()
}

/**
 * Manages Google TV Streamer Direct Wi-Fi Connection via Android TV Remote v2 Protocol.
 * Handles mDNS discovery, Port 6467 TLS pairing, and Port 6466 TLS command socket.
 */
class GtvManager(
    private val context: Context,
    private val preferences: AppPreferences,
    private val remoteId: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<GtvState>(GtvState.Disconnected)
    val state: StateFlow<GtvState> = _state.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<GtvDiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<GtvDiscoveredDevice>> = _discoveredDevices.asStateFlow()

    // Pairing socket session
    private var pairingSocket: SSLSocket? = null
    private var pairingServerCert: X509Certificate? = null
    private var pairingHost: String? = null
    private var pairingName: String = "Google TV Streamer"

    // Command socket session
    private var commandSocket: SSLSocket? = null
    private var commandOut: OutputStream? = null
    private var commandReaderJob: Job? = null
    private var connectionJob: Job? = null
    private var connectionWatchdog: Job? = null
    private var attemptId: Long = 0
    private val sendLock = Any()

    // NSD Discovery
    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    init {
        // Auto-connect if previously paired
        val savedHost = preferences.getGoogleTvHost(remoteId)
        val isPaired = preferences.isGoogleTvPaired(remoteId)
        if (!savedHost.isNullOrEmpty() && isPaired) {
            connect(savedHost, preferences.getGoogleTvName(remoteId))
        }
    }

    private fun cleanHost(rawHost: String): String {
        val host = rawHost.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        return when {
            host.startsWith("[") -> host.substringAfter('[').substringBefore(']')
            host.count { it == ':' } == 1 -> host.substringBefore(':')
            else -> host // Preserve IPv6 addresses returned by mDNS.
        }
    }

    // --- Device Discovery ---

    fun startDiscovery() {
        stopDiscovery()
        _state.value = GtvState.Discovering(emptyList())
        _discoveredDevices.value = emptyList()

        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("GtvMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {
                    Log.d(TAG, "mDNS Discovery started for $regType")
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    Log.d(TAG, "mDNS Service found: ${serviceInfo.serviceName}, type: ${serviceInfo.serviceType}")
                    try {
                        nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onServiceResolved(resolved: NsdServiceInfo) {
                                val host = resolved.host?.hostAddress ?: return
                                val name = resolved.serviceName ?: "Google TV Streamer"
                                val dev = GtvDiscoveredDevice(name = name, host = host, port = resolved.port)
                                Log.d(TAG, "mDNS Service resolved: $name @ $host:${resolved.port}")

                                val current = _discoveredDevices.value.toMutableList()
                                if (current.none { it.host == host }) {
                                    current.add(dev)
                                    _discoveredDevices.value = current
                                    if (_state.value is GtvState.Discovering) {
                                        _state.value = GtvState.Discovering(current)
                                    }
                                }
                            }

                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                Log.w(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                            }
                        })
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resolving service", e)
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    val current = _discoveredDevices.value.toMutableList()
                    current.removeAll { it.name == serviceInfo.serviceName }
                    _discoveredDevices.value = current
                }

                override fun onDiscoveryStopped(serviceType: String) {
                    Log.d(TAG, "mDNS Discovery stopped")
                }

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e(TAG, "Start discovery failed: $errorCode")
                    stopDiscovery()
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e(TAG, "Stop discovery failed: $errorCode")
                }
            }

            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discovery", e)
        }
    }

    fun stopDiscovery() {
        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {}
        discoveryListener = null
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }

    private fun armConnectionWatchdog(host: String, timeoutMs: Long, message: String) {
        val attempt = ++attemptId
        connectionWatchdog?.cancel()
        connectionWatchdog = scope.launch {
            delay(timeoutMs)
            if (attempt == attemptId && _state.value is GtvState.Connecting) {
                Log.w(TAG, "Connection watchdog expired for $host")
                connectionJob?.cancel()
                commandReaderJob?.cancel()
                try { pairingSocket?.close() } catch (_: Exception) {}
                try { commandSocket?.close() } catch (_: Exception) {}
                pairingSocket = null
                commandSocket = null
                commandOut = null
                withContext(Dispatchers.Main) {
                    if (attempt == attemptId && _state.value is GtvState.Connecting) {
                        _state.value = GtvState.Error(message, host)
                    }
                }
            }
        }
    }

    private fun clearConnectionWatchdog() {
        attemptId++
        connectionWatchdog?.cancel()
        connectionWatchdog = null
    }

    // --- Pairing Flow (Port 6467) ---

    fun startPairing(host: String, tvName: String = "Google TV Streamer") {
        val targetHost = cleanHost(host)
        if (targetHost.isBlank()) {
            _state.value = GtvState.Error("IP address cannot be empty")
            return
        }

        stopDiscovery()
        connectionJob?.cancel()
        disconnectInternal()

        pairingHost = targetHost
        pairingName = tvName
        _state.value = GtvState.Connecting(targetHost)
        armConnectionWatchdog(targetHost, 40_000, "Pairing setup timed out. Check that the TV is awake, then retry.")

        connectionJob = scope.launch {
            try {
                Log.d(TAG, "Connecting to pairing port $targetHost:$PORT_PAIRING...")
                val sslContext = GtvCrypto.createSslContext { serverCert ->
                    pairingServerCert = serverCert
                }

                val socket = sslContext.socketFactory.createSocket() as SSLSocket
                socket.tcpNoDelay = true
                socket.soTimeout = 30000 // 30s timeout for user to type code
                pairingSocket = socket
                socket.connect(InetSocketAddress(targetHost, PORT_PAIRING), 8000)
                socket.startHandshake()

                val out = socket.outputStream
                val inStream = socket.inputStream

                // Step 1: Send PairingRequest
                Log.d(TAG, "Sending Polo PairingRequest...")
                val reqPayload = GtvWire.buildPairingRequest(serviceName = "atvremote", clientName = "iRed")
                val reqMsg = GtvWire.buildOuterMessage(type = GtvWire.POLO_TYPE_PAIRING_REQUEST, payload = reqPayload)
                GtvWire.writeDelimitedMessage(out, reqMsg)

                // Read PairingRequestAck
                val ackBytes = GtvWire.readDelimitedMessage(inStream)
                GtvWire.validatePairingResponse(ackBytes, GtvWire.POLO_TYPE_PAIRING_REQUEST_ACK)

                GtvWire.writeDelimitedMessage(out, GtvWire.buildOuterMessage(
                    GtvWire.POLO_TYPE_OPTIONS, GtvWire.buildOptions()))
                GtvWire.validatePairingResponse(
                    GtvWire.readDelimitedMessage(inStream), GtvWire.POLO_TYPE_OPTIONS)

                // Step 2: Send Configuration (Hexadecimal, 6 symbols, Input role)
                Log.d(TAG, "Sending Polo Configuration...")
                val configPayload = GtvWire.buildConfiguration(
                    encodingType = GtvWire.POLO_ENCODING_HEXADECIMAL,
                    symbolLength = 6,
                    clientRole = GtvWire.POLO_ROLE_INPUT
                )
                val configMsg = GtvWire.buildOuterMessage(type = GtvWire.POLO_TYPE_CONFIGURATION, payload = configPayload)
                GtvWire.writeDelimitedMessage(out, configMsg)

                // Read ConfigurationAck
                val configAckBytes = GtvWire.readDelimitedMessage(inStream)
                GtvWire.validatePairingResponse(configAckBytes, GtvWire.POLO_TYPE_CONFIGURATION_ACK)

                // At this point, the Google TV Streamer is displaying the 6-character PIN on screen!
                withContext(Dispatchers.Main) {
                    clearConnectionWatchdog()
                    _state.value = GtvState.PairingPrompt(targetHost, tvName)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Pairing initiation failed", e)
                closePairingSocket()
                val errorMsg = when (e) {
                    is ConnectException -> "Connection refused at $targetHost:$PORT_PAIRING. Ensure your Google TV is powered ON, awake, and connected to the same Wi-Fi."
                    is SocketTimeoutException -> "Timed out connecting to $targetHost:$PORT_PAIRING. Check that the IP address matches your TV."
                    is SSLHandshakeException -> "TLS Handshake failed with $targetHost: ${e.message}"
                    else -> "Pairing error (${e::class.java.simpleName}): ${e.localizedMessage}"
                }
                clearConnectionWatchdog()
                withContext(Dispatchers.Main) {
                    _state.value = GtvState.Error(errorMsg, targetHost)
                }
            }
        }
    }

    /**
     * Completes the pairing handshake using the 6-character hex PIN displayed on TV.
     */
    fun submitPairingPin(pin: String) {
        val socket = pairingSocket
        val host = pairingHost
        val serverCert = pairingServerCert

        if (socket == null || host == null || serverCert == null) {
            _state.value = GtvState.Error("No active pairing session", host)
            return
        }

        if (_state.value !is GtvState.PairingPrompt) return
        _state.value = GtvState.Connecting(host)
        armConnectionWatchdog(host, 25_000, "TV did not complete PIN verification in time. Cancel and pair again.")
        connectionJob = scope.launch {
            try {
                Log.d(TAG, "Calculating pairing secret")
                val (_, clientCert) = GtvCrypto.getOrCreateClientCredentials()
                val secretDigest = GtvCrypto.calculateSecret(clientCert, serverCert, pin)

                val out = socket.outputStream
                val inStream = socket.inputStream

                // Step 3: Send Secret
                Log.d(TAG, "Sending Polo Secret...")
                val secretPayload = GtvWire.buildSecret(secretDigest)
                val secretMsg = GtvWire.buildOuterMessage(type = GtvWire.POLO_TYPE_SECRET, payload = secretPayload)
                GtvWire.writeDelimitedMessage(out, secretMsg)

                // Read SecretAck
                val secretAckBytes = GtvWire.readDelimitedMessage(inStream)
                GtvWire.validatePairingResponse(secretAckBytes, GtvWire.POLO_TYPE_SECRET_ACK)

                closePairingSocket()

                // Save paired info
                preferences.setGoogleTvHost(remoteId, host)
                preferences.setGoogleTvPaired(remoteId, true)
                preferences.setGoogleTvName(remoteId, pairingName)
                pairingServerCert?.let { cert ->
                    preferences.setGoogleTvFingerprint(remoteId, MessageDigest.getInstance("SHA-256").digest(cert.encoded)
                        .joinToString("") { "%02X".format(it) })
                }
                Log.d(TAG, "Pairing completed successfully for $host ($pairingName)!")

                // Immediately connect to command port
                connect(host, pairingName)

            } catch (e: Exception) {
                Log.e(TAG, "Pairing secret verification failed", e)
                closePairingSocket()
                val errorMsg = when (e) {
                    is IllegalArgumentException -> e.message ?: "Invalid PIN code"
                    else -> "Pairing verification failed: ${e.localizedMessage}"
                }
                withContext(Dispatchers.Main) {
                    _state.value = GtvState.Error(errorMsg, host)
                }
            }
        }
    }

    private fun closePairingSocket() {
        try {
            pairingSocket?.close()
        } catch (_: Exception) {}
        pairingSocket = null
    }

    // --- Command Session (Port 6466) ---

    fun connect(host: String, tvName: String = "Google TV Streamer") {
        val targetHost = cleanHost(host)
        if (targetHost.isBlank()) {
            _state.value = GtvState.Error("IP address cannot be empty")
            return
        }

        stopDiscovery()
        connectionJob?.cancel()
        disconnectInternal()

        _state.value = GtvState.Connecting(targetHost)
        armConnectionWatchdog(targetHost, 20_000, "TV did not finish starting the remote session. Check the TV and retry.")

        connectionJob = scope.launch {
            try {
                Log.d(TAG, "Connecting to command port $targetHost:$PORT_COMMAND...")
                var peerFingerprint: String? = null
                val sslContext = GtvCrypto.createSslContext { cert ->
                    peerFingerprint = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
                        .joinToString("") { "%02X".format(it) }
                }
                val socket = sslContext.socketFactory.createSocket() as SSLSocket
                socket.tcpNoDelay = true
                socket.soTimeout = 15000
                commandSocket = socket
                socket.connect(InetSocketAddress(targetHost, PORT_COMMAND), 8000)
                socket.startHandshake()
                val savedFingerprint = preferences.getGoogleTvFingerprint(remoteId)
                if (savedFingerprint != null && savedFingerprint != peerFingerprint) {
                    socket.close()
                    throw SSLHandshakeException("TV identity changed; unpair and pair again")
                }
                val out = socket.outputStream
                val inStream = socket.inputStream
                commandOut = out

                // Start background reader loop for heartbeats
                startCommandReader(inStream, out, targetHost, tvName)

            } catch (e: Exception) {
                Log.e(TAG, "Command connection failed for $targetHost", e)
                disconnectInternal()
                val errorMsg = when (e) {
                    is ConnectException -> "Connection refused at $targetHost:$PORT_COMMAND. If device is not paired yet, tap 'Pair with PIN'."
                    is SocketTimeoutException -> "Timed out connecting to $targetHost:$PORT_COMMAND. Check TV's IP address."
                    is SSLHandshakeException -> "TLS Authentication failed with $targetHost. Please re-pair the device."
                    else -> "Connection failed (${e::class.java.simpleName}): ${e.localizedMessage}"
                }
                clearConnectionWatchdog()
                withContext(Dispatchers.Main) {
                    _state.value = GtvState.Error(errorMsg, targetHost)
                }
            }
        }
    }

    private fun startCommandReader(
        inStream: InputStream,
        out: OutputStream,
        host: String,
        tvName: String
    ) {
        commandReaderJob?.cancel()
        commandReaderJob = scope.launch {
            try {
                var activeFeatures = 615 // Match the Android TV Remote v2 client feature negotiation (PING, KEY, IME, POWER, VOLUME, APP_LINK).
                while (isActive) {
                    val msgBytes = GtvWire.readDelimitedMessage(inStream)
                    val fields = GtvWire.parseProtoFields(msgBytes)
                    Log.d(TAG, "Command frame received with fields: ${fields.joinToString { it.number.toString() }}")

                    fields.find { it.number == 1 && it.bytesValue != null }?.let { configure ->
                        val supported = GtvWire.parseProtoFields(configure.bytesValue!!)
                            .find { it.number == 1 }?.varintValue?.toInt() ?: 0
                        require(supported and 2 != 0) { "TV does not support remote keys" }
                        activeFeatures = activeFeatures and supported
                        synchronized(sendLock) {
                            GtvWire.writeDelimitedMessage(out, GtvWire.buildRemoteConfigure(activeFeatures, "iRed", "iRed"))
                            Log.d(TAG, "Replied to RemoteConfigure with feature mask $activeFeatures")
                        }
                    }
                    if (fields.any { it.number == 2 }) {
                        synchronized(sendLock) {
                            GtvWire.writeDelimitedMessage(out, GtvWire.buildRemoteSetActive(activeFeatures))
                            Log.d(TAG, "Replied to RemoteSetActive with feature mask $activeFeatures")
                        }
                    }
                    if (fields.any { it.number == 3 }) error("TV rejected the remote handshake. Check Android TV Remote Service, then retry.")
                    if (fields.any { it.number == 40 }) {
                        commandSocket?.soTimeout = 0
                        clearConnectionWatchdog()
                        _state.value = GtvState.Connected(host, tvName)
                    }

                    // Check for RemotePingRequest (field 8)
                    val pingReqField = fields.find { it.number == 8 }
                    if (pingReqField != null && pingReqField.bytesValue != null) {
                        val pingFields = GtvWire.parseProtoFields(pingReqField.bytesValue)
                        val val1 = pingFields.find { it.number == 1 }?.varintValue?.toInt() ?: 0
                        Log.d(TAG, "Ping received (val1=$val1), responding with RemotePingResponse...")

                        val pongMsg = GtvWire.buildRemotePingResponse(val1)
                        synchronized(sendLock) {
                            GtvWire.writeDelimitedMessage(out, pongMsg)
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.w(TAG, "Command socket read loop ended: ${e.localizedMessage}")
                    // Do not call disconnectInternal() here: it cancels commandReaderJob,
                    // which is this coroutine, and would cancel the UI state update below.
                    commandReaderJob = null
                    try { commandSocket?.close() } catch (_: Exception) {}
                    commandSocket = null
                    commandOut = null
                    clearConnectionWatchdog()
                    withContext(Dispatchers.Main) {
                        _state.value = GtvState.Error(
                            "Remote session failed: ${e.localizedMessage ?: e.javaClass.simpleName}. " +
                                "Retry pairing or check that the TV is awake and on the same Wi-Fi.",
                            host
                        )
                    }
                }
            }
        }
    }

    // --- Remote Control Key Injections ---

    fun sendKey(keyCode: Int, direction: Int = GtvWire.REMOTE_DIRECTION_SHORT): Boolean {
        val out = commandOut
        if (out == null || commandSocket == null || _state.value !is GtvState.Connected) {
            Log.w(TAG, "Cannot send key $keyCode: Not connected to Google TV")
            return false
        }

        scope.launch {
            try {
                val keyMsg = GtvWire.buildRemoteKeyInject(keyCode, direction)
                synchronized(sendLock) {
                    GtvWire.writeDelimitedMessage(out, keyMsg)
                }
                Log.d(TAG, "Injected key $keyCode (direction $direction)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send key $keyCode", e)
            }
        }
        return true
    }

    // Quick helpers
    fun dpadUp() = sendKey(GtvWire.KEYCODE_DPAD_UP)
    fun dpadDown() = sendKey(GtvWire.KEYCODE_DPAD_DOWN)
    fun dpadLeft() = sendKey(GtvWire.KEYCODE_DPAD_LEFT)
    fun dpadRight() = sendKey(GtvWire.KEYCODE_DPAD_RIGHT)
    fun dpadCenter() = sendKey(GtvWire.KEYCODE_DPAD_CENTER)
    fun back() = sendKey(GtvWire.KEYCODE_BACK)
    fun home() = sendKey(GtvWire.KEYCODE_HOME)
    fun volumeUp() = sendKey(GtvWire.KEYCODE_VOLUME_UP)
    fun volumeDown() = sendKey(GtvWire.KEYCODE_VOLUME_DOWN)
    fun mute() = sendKey(GtvWire.KEYCODE_VOLUME_MUTE)
    fun power() = sendKey(GtvWire.KEYCODE_POWER)
    fun settings() = sendKey(GtvWire.KEYCODE_SETTINGS)
    fun assistant() = sendKey(GtvWire.KEYCODE_VOICE_ASSIST)
    fun playPause() = sendKey(GtvWire.KEYCODE_MEDIA_PLAY_PAUSE)

    fun cancelConnection() {
        clearConnectionWatchdog()
        stopDiscovery()
        connectionJob?.cancel()
        connectionJob = null
        disconnectInternal()
        _state.value = GtvState.Disconnected
    }

    fun disconnect() {
        clearConnectionWatchdog()
        stopDiscovery()
        connectionJob?.cancel()
        connectionJob = null
        disconnectInternal()
        _state.value = GtvState.Disconnected
    }

    fun unpair() {
        disconnect()
        connectionJob?.cancel()
        preferences.clearGoogleTvProfile(remoteId)
        _state.value = GtvState.Disconnected
    }

    fun close() {
        disconnect()
        scope.cancel()
    }

    private fun disconnectInternal() {
        commandReaderJob?.cancel()
        commandReaderJob = null
        try {
            commandSocket?.close()
        } catch (_: Exception) {}
        commandSocket = null
        commandOut = null
        closePairingSocket()
    }
}
