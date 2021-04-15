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
import net.corda.networkcloner.impl.SerializerImpl
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

    val serializer = SerializerImpl(Paths.get("/Users/alex.koller/Projects/contract-sdk/examples/test-app/buildDestination/nodes/Operator/cordapps"))
    val transactionStore = TransactionsStoreImpl("jdbc:h2:/Users/alex.koller/Projects/contract-sdk/examples/test-app/buildSource/nodes/Operator/persistence", "sa", "")
    val transactions = transactionStore.getAllTransactions()
    val identityMapper = IdentityMapperImpl(emptyList(), emptyList())
    val signer = SignerImpl(identityMapper)


    transactions.forEach {
        val deserialized = serializer.deserializeDbBlobIntoTransaction(it)

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

        val destinationWireTransaction = WireTransaction(componentGroups, wTx.privacySalt, wTx.digestService)



        val newSignatures = signer.sign(destinationWireTransaction, deserialized.sigs.map { it.by })


        val sTx = SignedTransaction(destinationWireTransaction, deserialized.sigs)

        val serializedFromSignedTx = sTx.serialize(context = SerializationDefaults.STORAGE_CONTEXT.withEncoding(CordaSerializationEncoding.SNAPPY))

        println("Db tx and destination tx are same: ${it.contentEquals(serializedFromSignedTx.bytes)}")

    }

    println("Done")


}