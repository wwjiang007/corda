package net.corda.networkcloner

import com.github.benmanes.caffeine.cache.Caffeine
import net.corda.core.internal.createComponentGroups
import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.internal.SerializationEnvironment
import net.corda.core.serialization.internal.nodeSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.impl.IdentityMapperImpl
import net.corda.networkcloner.impl.SignerImpl
import net.corda.networkcloner.impl.TransactionsStoreImpl
import net.corda.node.VersionInfo
import net.corda.node.internal.cordapp.JarScanningCordappLoader
import net.corda.nodeapi.internal.rpc.client.AMQPClientSerializationScheme
import net.corda.nodeapi.internal.serialization.amqp.AMQPServerSerializationScheme
import net.corda.serialization.internal.AMQP_P2P_CONTEXT
import net.corda.serialization.internal.AMQP_STORAGE_CONTEXT
import net.corda.serialization.internal.CordaSerializationEncoding
import net.corda.serialization.internal.CordaSerializationMagic
import net.corda.serialization.internal.SerializationFactoryImpl
import net.corda.serialization.internal.amqp.AbstractAMQPSerializationScheme
import net.corda.serialization.internal.amqp.SerializationFactoryCacheKey
import net.corda.serialization.internal.amqp.SerializerFactory
import net.corda.serialization.internal.amqp.amqpMagic
import java.nio.file.Paths

fun main(args: Array<String>) {

    println("Hello there")

    val transactionStore = TransactionsStoreImpl()
    val transactions = transactionStore.getAllTransactions()
    val identityMapper = IdentityMapperImpl()
    val signer = SignerImpl(identityMapper)


    initialiseSerialization()

    transactions.forEach {
        val deserialized = it.deserialize<Any>(context = SerializationDefaults.STORAGE_CONTEXT)

        val wTx = (deserialized as SignedTransaction).coreTransaction as WireTransaction

        //edit the component groups start

        val componentGroups =
            createComponentGroups(wTx.inputs, wTx.outputs, wTx.commands, wTx.attachments, wTx.notary, wTx.timeWindow, wTx.references, wTx.networkParametersHash)


        wTx.componentGroups.forEachIndexed {
            i, v ->
            val componentGroupCreated = componentGroups[i]
            println("Are they the same for ${v.groupIndex}: ${componentGroupCreated.components == v.components}")
        }

        //edit the component groups end

        val destinationWireTransaction = WireTransaction(wTx.componentGroups, wTx.privacySalt, wTx.digestService)



        val newSignatures = signer.sign(destinationWireTransaction, deserialized.sigs.map { it.by })


        val sTx = SignedTransaction(destinationWireTransaction, newSignatures)

        val serializedFromSignedTx = sTx.serialize(context = SerializationDefaults.STORAGE_CONTEXT.withEncoding(CordaSerializationEncoding.SNAPPY))

    }

    println("Done")


}

private fun initialiseSerialization() {
    val cordappLoader = JarScanningCordappLoader.fromDirectories(
            listOf(Paths.get("/Users/alex.koller/Projects/contract-sdk/examples/test-app/buildDestination/nodes/Operator/cordapps")),
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

private object AMQPInspectorSerializationScheme : AbstractAMQPSerializationScheme(emptyList()) {
    override fun canDeserializeVersion(magic: CordaSerializationMagic, target: SerializationContext.UseCase): Boolean {
        return magic == amqpMagic
    }

    override fun rpcClientSerializerFactory(context: SerializationContext) = throw UnsupportedOperationException()
    override fun rpcServerSerializerFactory(context: SerializationContext) = throw UnsupportedOperationException()
}