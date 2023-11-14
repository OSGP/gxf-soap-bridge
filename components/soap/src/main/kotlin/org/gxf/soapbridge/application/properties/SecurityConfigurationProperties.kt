// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge.application.properties

import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

@ConfigurationProperties("security")
class SecurityConfigurationProperties(
    val keyStore: StoreConfigurationProperties,
    val trustStore: StoreConfigurationProperties,
    val signing: SigningConfigurationProperties
)

class StoreConfigurationProperties(
    val location: String,
    val password: String,
    val type: String
)


class SigningConfigurationProperties(
    val keyType: String,
    val signKeyFile: String,
    val verifyKeyFile: String,
    /** Indicates which provider is used for signing and verification. */
    val provider: String,
    /** Indicates which signature is used for signing and verification. */
    val signature: String
) {
    private val logger = KotlinLogging.logger {}

    /** Private key used for signing. */
    val signKey = createPrivateKey(signKeyFile, keyType, provider)

    /** Public key used for verification. */
    val verifyKey = createPublicKey(verifyKeyFile, keyType, provider)

    private fun createPrivateKey(
        keyPath: String, keyType: String, provider: String
    ): PrivateKey? {
        return try {
            val key = readKeyFromDisk(keyPath)
            val privateKeySpec = PKCS8EncodedKeySpec(key)
            val privateKeyFactory = KeyFactory.getInstance(keyType, provider)
            privateKeyFactory.generatePrivate(privateKeySpec)
        } catch (e: Exception) {
            logger.error("Unexpected exception during private key creation", e)
            null
        }
    }

    private fun createPublicKey(
        keyPath: String, keyType: String, provider: String
    ): PublicKey? {
        return try {
            val key = readKeyFromDisk(keyPath)
            val publicKeySpec = X509EncodedKeySpec(key)
            val publicKeyFactory = KeyFactory.getInstance(keyType, provider)
            publicKeyFactory.generatePublic(publicKeySpec)
        } catch (e: Exception) {
            logger.error("Unexpected exception during public key creation", e)
            null
        }
    }

    @Throws(IOException::class)
    private fun readKeyFromDisk(keyPath: String): ByteArray = Files.readAllBytes(Paths.get(keyPath))
}
