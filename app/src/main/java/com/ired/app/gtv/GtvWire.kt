package com.ired.app.gtv

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/**
 * Lightweight Protobuf wire format serializer and parser for Android TV Remote v2 & Polo protocols.
 * Zero external dependencies.
 */
object GtvWire {

    // Wire types
    const val WIRE_VARINT = 0
    const val WIRE_FIXED64 = 1
    const val WIRE_LENGTH_DELIMITED = 2
    const val WIRE_FIXED32 = 5

    // Polo Message Types
    const val POLO_TYPE_PAIRING_REQUEST = 10
    const val POLO_TYPE_PAIRING_REQUEST_ACK = 11
    const val POLO_TYPE_OPTIONS = 20
    const val POLO_TYPE_CONFIGURATION = 30
    const val POLO_TYPE_CONFIGURATION_ACK = 31
    const val POLO_TYPE_SECRET = 40
    const val POLO_TYPE_SECRET_ACK = 41

    // Polo Status
    const val POLO_STATUS_OK = 200

    // Polo Encoding & Role
    const val POLO_ENCODING_HEXADECIMAL = 3
    const val POLO_ROLE_INPUT = 1

    // Remote Directions
    const val REMOTE_DIRECTION_START_LONG = 1
    const val REMOTE_DIRECTION_END_LONG = 2
    const val REMOTE_DIRECTION_SHORT = 3

    // Common Android KeyCodes used by Google TV
    const val KEYCODE_HOME = 3
    const val KEYCODE_SEARCH = 84
    const val KEYCODE_BACK = 4
    const val KEYCODE_DPAD_UP = 19
    const val KEYCODE_DPAD_DOWN = 20
    const val KEYCODE_DPAD_LEFT = 21
    const val KEYCODE_DPAD_RIGHT = 22
    const val KEYCODE_DPAD_CENTER = 23
    const val KEYCODE_VOLUME_UP = 24
    const val KEYCODE_VOLUME_DOWN = 25
    const val KEYCODE_POWER = 26
    const val KEYCODE_MEDIA_PLAY_PAUSE = 85
    const val KEYCODE_VOLUME_MUTE = 164
    const val KEYCODE_SETTINGS = 176
    const val KEYCODE_VOICE_ASSIST = 231

    // --- Varint Utilities ---

    fun writeVarint(out: OutputStream, value: Long) {
        var v = value
        while ((v and 0x7FL.inv()) != 0L) {
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        out.write((v and 0x7F).toInt())
    }

    fun readVarint(inStream: InputStream): Long {
        var result = 0L
        var shift = 0
        while (true) {
            val b = inStream.read()
            if (b == -1) throw EOFException("End of stream while reading varint")
            result = result or ((b.toLong() and 0x7F) shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
            if (shift >= 64) throw IllegalArgumentException("Varint too long")
        }
        return result
    }

    fun writeDelimitedMessage(out: OutputStream, messageBytes: ByteArray) {
        writeVarint(out, messageBytes.size.toLong())
        out.write(messageBytes)
        out.flush()
    }

    fun readDelimitedMessage(inStream: InputStream): ByteArray {
        val size = readVarint(inStream)
        require(size in 0..1_048_576L) { "Invalid protobuf frame size: $size" }
        val length = size.toInt()
        val buffer = ByteArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = inStream.read(buffer, totalRead, length - totalRead)
            if (read == -1) throw EOFException("Unexpected EOF reading delimited protobuf message")
            totalRead += read
        }
        return buffer
    }

    // --- Protobuf Serialization Helpers ---

    private fun writeTag(out: ByteArrayOutputStream, fieldNumber: Int, wireType: Int) {
        val tag = (fieldNumber shl 3) or wireType
        writeVarint(out, tag.toLong())
    }

    fun writeFieldVarint(out: ByteArrayOutputStream, fieldNumber: Int, value: Long) {
        writeTag(out, fieldNumber, WIRE_VARINT)
        writeVarint(out, value)
    }

    fun writeFieldBytes(out: ByteArrayOutputStream, fieldNumber: Int, bytes: ByteArray) {
        writeTag(out, fieldNumber, WIRE_LENGTH_DELIMITED)
        writeVarint(out, bytes.size.toLong())
        out.write(bytes)
    }

    fun writeFieldString(out: ByteArrayOutputStream, fieldNumber: Int, str: String) {
        writeFieldBytes(out, fieldNumber, str.toByteArray(Charsets.UTF_8))
    }

    fun writeFieldMessage(out: ByteArrayOutputStream, fieldNumber: Int, messageBytes: ByteArray) {
        writeFieldBytes(out, fieldNumber, messageBytes)
    }

    // --- Polo Protocol Messages ---

    fun buildOuterMessage(
        type: Int,
        payload: ByteArray,
        protocolVersion: Int = 2,
        status: Int = POLO_STATUS_OK
    ): ByteArray {
        val out = ByteArrayOutputStream()
        writeFieldVarint(out, 1, protocolVersion.toLong())
        writeFieldVarint(out, 2, status.toLong())
        writeFieldBytes(out, type, payload)
        return out.toByteArray()
    }

    fun buildPairingRequest(serviceName: String = "atvremote", clientName: String = "iRed"): ByteArray {
        val out = ByteArrayOutputStream()
        writeFieldString(out, 1, serviceName)
        writeFieldString(out, 2, clientName)
        return out.toByteArray()
    }

    fun buildOptions(): ByteArray {
        val encoding = ByteArrayOutputStream()
        writeFieldVarint(encoding, 1, POLO_ENCODING_HEXADECIMAL.toLong())
        writeFieldVarint(encoding, 2, 6)
        val options = ByteArrayOutputStream()
        writeFieldMessage(options, 1, encoding.toByteArray())
        writeFieldVarint(options, 3, POLO_ROLE_INPUT.toLong())
        return options.toByteArray()
    }

    fun validatePairingResponse(message: ByteArray, expectedField: Int) {
        val fields = parseProtoFields(message)
        val status = fields.find { it.number == 2 }?.varintValue
        require(status == POLO_STATUS_OK.toLong()) {
            "TV rejected pairing (status $status). Start pairing again."
        }
        require(fields.any { it.number == expectedField && it.bytesValue != null }) {
            "Unexpected pairing response; expected field $expectedField"
        }
    }

    fun buildConfiguration(
        encodingType: Int = POLO_ENCODING_HEXADECIMAL,
        symbolLength: Int = 6,
        clientRole: Int = POLO_ROLE_INPUT
    ): ByteArray {
        val encodingOut = ByteArrayOutputStream()
        writeFieldVarint(encodingOut, 1, encodingType.toLong())
        writeFieldVarint(encodingOut, 2, symbolLength.toLong())

        val configOut = ByteArrayOutputStream()
        writeFieldMessage(configOut, 1, encodingOut.toByteArray())
        writeFieldVarint(configOut, 2, clientRole.toLong())
        return configOut.toByteArray()
    }

    fun buildSecret(secretDigest: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        writeFieldBytes(out, 1, secretDigest)
        return out.toByteArray()
    }

    // --- Remote Control Messages (Port 6466) ---

    fun buildRemoteConfigure(
        code1: Int = 622,
        model: String = "iRed Remote",
        vendor: String = "Google"
    ): ByteArray {
        val devInfo = ByteArrayOutputStream()
        writeFieldString(devInfo, 1, model)
        writeFieldString(devInfo, 2, vendor)
        writeFieldVarint(devInfo, 3, 1)
        writeFieldString(devInfo, 4, "1")
        writeFieldString(devInfo, 5, "atvremote")
        writeFieldString(devInfo, 6, "1.0.0")

        val config = ByteArrayOutputStream()
        writeFieldVarint(config, 1, code1.toLong())
        writeFieldMessage(config, 2, devInfo.toByteArray())

        val remoteMessage = ByteArrayOutputStream()
        writeFieldMessage(remoteMessage, 1, config.toByteArray())
        return remoteMessage.toByteArray()
    }

    fun buildRemoteSetActive(active: Int = 622): ByteArray {
        val setActive = ByteArrayOutputStream()
        writeFieldVarint(setActive, 1, active.toLong())

        val remoteMessage = ByteArrayOutputStream()
        writeFieldMessage(remoteMessage, 2, setActive.toByteArray())
        return remoteMessage.toByteArray()
    }

    fun buildRemoteKeyInject(keyCode: Int, direction: Int = REMOTE_DIRECTION_SHORT): ByteArray {
        val keyInject = ByteArrayOutputStream()
        writeFieldVarint(keyInject, 1, keyCode.toLong())
        writeFieldVarint(keyInject, 2, direction.toLong())

        val remoteMessage = ByteArrayOutputStream()
        writeFieldMessage(remoteMessage, 10, keyInject.toByteArray())
        return remoteMessage.toByteArray()
    }

    fun buildRemotePingResponse(val1: Int): ByteArray {
        val pingResponse = ByteArrayOutputStream()
        writeFieldVarint(pingResponse, 1, val1.toLong())

        val remoteMessage = ByteArrayOutputStream()
        writeFieldMessage(remoteMessage, 9, pingResponse.toByteArray())
        return remoteMessage.toByteArray()
    }

    // --- Simple Protobuf Deserialization ---

    data class ProtoField(
        val number: Int,
        val wireType: Int,
        val varintValue: Long? = null,
        val bytesValue: ByteArray? = null
    )

    fun parseProtoFields(data: ByteArray): List<ProtoField> {
        val input = data.inputStream()
        val fields = mutableListOf<ProtoField>()
        while (input.available() > 0) {
            val tag = readVarint(input)
            val number = (tag ushr 3).toInt()
            val wireType = (tag and 7).toInt()
            require(number > 0) { "Invalid protobuf field" }
            when (wireType) {
                WIRE_VARINT -> fields.add(ProtoField(number, wireType, varintValue = readVarint(input)))
                WIRE_LENGTH_DELIMITED -> {
                    val length = readVarint(input)
                    require(length in 0..input.available().toLong()) { "Truncated protobuf field" }
                    val bytes = ByteArray(length.toInt())
                    require(input.read(bytes) == bytes.size || bytes.isEmpty()) { "Truncated protobuf field" }
                    fields.add(ProtoField(number, wireType, bytesValue = bytes))
                }
                WIRE_FIXED32, WIRE_FIXED64 -> {
                    val length = if (wireType == WIRE_FIXED32) 4 else 8
                    require(input.available() >= length) { "Truncated protobuf field" }
                    input.skip(length.toLong())
                }
                else -> throw IllegalArgumentException("Unsupported protobuf wire type $wireType")
            }
        }
        return fields
    }
}
