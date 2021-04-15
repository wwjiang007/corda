package net.corda.networkcloner.impl

import com.github.benmanes.caffeine.cache.Caffeine
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.internal.SerializationEnvironment
import net.corda.core.serialization.internal.nodeSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.networkcloner.api.Serializer
import net.corda.node.VersionInfo
import net.corda.node.internal.cordapp.JarScanningCordappLoader
import net.corda.nodeapi.internal.rpc.client.AMQPClientSerializationScheme
import net.corda.nodeapi.internal.serialization.amqp.AMQPServerSerializationScheme
import net.corda.serialization.internal.AMQP_P2P_CONTEXT
import net.corda.serialization.internal.AMQP_STORAGE_CONTEXT
import net.corda.serialization.internal.CordaSerializationEncoding
import net.corda.serialization.internal.SerializationFactoryImpl
import net.corda.serialization.internal.amqp.SerializationFactoryCacheKey
import net.corda.serialization.internal.amqp.SerializerFactory
import java.nio.file.Path

class SerializerImpl(destinationCordappsDirectory : Path) : Serializer {

    init {
        val cordappLoader = JarScanningCordappLoader.fromDirectories(
                listOf(destinationCordappsDirectory),
                VersionInfo.UNKNOWN,
                extraCordapps = emptyList(),
                signerKeyFingerprintBlacklist = emptyList()
        )
        val classloader = cordappLoader.appClassLoader
        nodeSerializationEnv = SerializationEnvironment.with(
                SerializationFactoryImpl().apply {
                    registerScheme(AMQPServerSerializationScheme(cordappLoader.cordapps, Caffeine.newBuilder().maximumSize(128).build<SerializationFactoryCacheKey, SerializerFactory>().asMap()))
                    registerScheme(AMQPClientSerializationScheme(cordappLoader.cordapps, Caffeine.newBuilder().maximumSize(128).build<SerializationFactoryCacheKey, SerializerFactory>().asMap()))
                },
                p2pContext = AMQP_P2P_CONTEXT.withClassLoader(classloader),
                storageContext = AMQP_STORAGE_CONTEXT.withClassLoader(classloader)
        )
    }

    override fun serializeSignedTransaction(signedTransaction: SignedTransaction): ByteArray {
        return signedTransaction.serialize(context = SerializationDefaults.STORAGE_CONTEXT.withEncoding(CordaSerializationEncoding.SNAPPY)).bytes
    }

    override fun deserializeDbBlobIntoTransaction(byteArray: ByteArray): SignedTransaction {
        return byteArray.deserialize(context = SerializationDefaults.STORAGE_CONTEXT)
    }
}