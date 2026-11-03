package com.ired.app.gtv

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Principal
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.util.Date
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

/**
 * Handles cryptographic operations for the Android TV Remote v2 & Polo protocols:
 * - Generates / loads hardware-backed RSA-2048 client keypair in AndroidKeyStore.
 * - Supports DIGEST_NONE, DIGEST_SHA256, RSA_PKCS1, and RSA_PSS required by Android 14/15 Conscrypt TLS.
 * - Extracts client & server RSA public key parameters (modulus, exponent).
 * - Computes SHA-256 pairing secret hash with checksum verification.
 * - Builds mTLS SSLContext using a custom X509ExtendedKeyManager.
 */
object GtvCrypto {

    private const val KEY_ALIAS = "ired_gtv_client_v3"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /**
     * Retrieves or generates an RSA-2048 keypair with self-signed certificate in AndroidKeyStore.
     * Generates with DIGEST_NONE, SHA256, SHA512 and PKCS1 + PSS paddings for full TLS 1.2/1.3 compatibility.
     */
    fun getOrCreateClientCredentials(): Pair<PrivateKey, X509Certificate> {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        // Clean up legacy v1 key if present to ensure proper TLS 1.3 capabilities
        if (keyStore.containsAlias("ired_gtv_client_v1")) {
            try { keyStore.deleteEntry("ired_gtv_client_v1") } catch (_: Exception) {}
        }

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
            val now = System.currentTimeMillis()
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setKeySize(2048)
                // DIGEST_NONE is critical: Conscrypt hashes internally and requests raw signing
                .setDigests(
                    KeyProperties.DIGEST_NONE,
                    KeyProperties.DIGEST_SHA1,
                    KeyProperties.DIGEST_SHA256,
                    KeyProperties.DIGEST_SHA384,
                    KeyProperties.DIGEST_SHA512
                )
                // Both PKCS1 and PSS are required for TLS 1.2 and TLS 1.3 ciphers
                .setSignaturePaddings(
                    KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
                    KeyProperties.SIGNATURE_PADDING_RSA_PSS
                )
                // Conscrypt's AndroidKeyStore RSA-PSS path uses a raw RSA private-key
                // operation internally; without NONE, TLS client auth fails in BoringSSL.
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setCertificateSubject(X500Principal("CN=atvremote, O=iRed, C=US"))
                .setCertificateSerialNumber(BigInteger.valueOf(now))
                .setCertificateNotBefore(Date(now - 1000L * 60 * 60 * 24))
                .setCertificateNotAfter(Date(now + 1000L * 60 * 60 * 24 * 365 * 25)) // 25 years
                .build()

            kpg.initialize(spec)
            kpg.generateKeyPair()
        }

        val privateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
        val cert = keyStore.getCertificate(KEY_ALIAS) as X509Certificate
        return Pair(privateKey, cert)
    }

    /**
     * Builds an SSLContext configured with our client certificate and a custom TrustManager.
     * Uses a custom X509ExtendedKeyManager so client cert is ALWAYS provided regardless
     * of server CertificateRequest issuer filters.
     */
    fun createSslContext(
        onServerCertReceived: ((X509Certificate) -> Unit)? = null
    ): SSLContext {
        val (privateKey, cert) = getOrCreateClientCredentials()

        val keyManager = object : X509ExtendedKeyManager() {
            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> =
                arrayOf("atvremote")

            override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?): String =
                "atvremote"

            override fun chooseEngineClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?): String =
                "atvremote"

            override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> =
                emptyArray()

            override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? =
                null

            override fun chooseEngineServerAlias(keyType: String?, issuers: Array<out Principal>?, engine: SSLEngine?): String? =
                null

            override fun getCertificateChain(alias: String?): Array<X509Certificate> =
                arrayOf(cert)

            override fun getPrivateKey(alias: String?): PrivateKey =
                privateKey
        }

        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                if (!chain.isNullOrEmpty()) {
                    onServerCertReceived?.invoke(chain[0])
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(arrayOf<KeyManager>(keyManager), arrayOf<TrustManager>(trustManager), SecureRandom())
        return sslContext
    }

    /**
     * Computes the SHA-256 pairing secret required by Google TV Polo pairing protocol.
     *
     * @param clientCert Client's X509 certificate.
     * @param serverCert TV's X509 certificate.
     * @param codeHex 6-character hex PIN displayed on TV (e.g., "3F89A1").
     * @return SHA-256 hash digest (32 bytes).
     * @throws IllegalArgumentException if the PIN checksum does not match.
     */
    fun calculateSecret(
        clientCert: X509Certificate,
        serverCert: X509Certificate,
        codeHex: String
    ): ByteArray {
        val cleanCode = codeHex.trim().replace(" ", "").uppercase()
        require(cleanCode.length == 6) { "PIN code must be exactly 6 hex characters" }

        val codeBytes = ByteArray(3) { i ->
            cleanCode.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }

        val clientKey = clientCert.publicKey as RSAPublicKey
        val serverKey = serverCert.publicKey as RSAPublicKey

        val clientModBytes = toUnsignedByteArray(clientKey.modulus)
        val clientExpBytes = toUnsignedByteArray(clientKey.publicExponent)
        val serverModBytes = toUnsignedByteArray(serverKey.modulus)
        val serverExpBytes = toUnsignedByteArray(serverKey.publicExponent)

        // The secret payload is the last 2 bytes of the code (4 hex characters)
        val secretBytes = byteArrayOf(codeBytes[1], codeBytes[2])

        val md = MessageDigest.getInstance("SHA-256")
        md.update(clientModBytes)
        md.update(clientExpBytes)
        md.update(serverModBytes)
        md.update(serverExpBytes)
        md.update(secretBytes)

        val digest = md.digest()

        // Checksum verification: first byte of digest must match first byte of code
        if (digest[0] != codeBytes[0]) {
            throw IllegalArgumentException("PIN checksum mismatch. Expected ${codeBytes[0].toInt() and 0xFF}, computed ${digest[0].toInt() and 0xFF}")
        }

        return digest
    }

    /**
     * Converts a BigInteger to an unsigned big-endian byte array (stripping any leading sign byte 0x00).
     */
    private fun toUnsignedByteArray(value: BigInteger): ByteArray {
        val bytes = value.toByteArray()
        return if (bytes.isNotEmpty() && bytes[0] == 0.toByte()) {
            bytes.copyOfRange(1, bytes.size)
        } else {
            bytes
        }
    }
}
