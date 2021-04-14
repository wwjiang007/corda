package net.corda.networkcloner

import net.corda.core.internal.createComponentGroups
import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.internal.SerializationEnvironment
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.impl.IdentityMapperImpl
import net.corda.networkcloner.impl.SignerImpl
import net.corda.networkcloner.impl.TransactionsStoreImpl
import net.corda.serialization.internal.AMQP_P2P_CONTEXT
import net.corda.serialization.internal.AMQP_STORAGE_CONTEXT
import net.corda.serialization.internal.CordaSerializationEncoding
import net.corda.serialization.internal.CordaSerializationMagic
import net.corda.serialization.internal.SerializationFactoryImpl
import net.corda.serialization.internal.amqp.AbstractAMQPSerializationScheme
import net.corda.serialization.internal.amqp.amqpMagic
import java.util.*

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

        val componentGroups = createComponentGroups(wTx.inputs, wTx.outputs, wTx.commands, wTx.attachments, wTx.notary, wTx.timeWindow, wTx.references, wTx.networkParametersHash)

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
    // Deserialise with the lenient carpenter as we only care for the AMQP field getters
    _contextSerializationEnv.set(SerializationEnvironment.with(
            SerializationFactoryImpl().apply {
                registerScheme(AMQPInspectorSerializationScheme)
            },
            p2pContext = AMQP_P2P_CONTEXT.withLenientCarpenter(),
            storageContext = AMQP_STORAGE_CONTEXT.withLenientCarpenter()
    ))
}

private object AMQPInspectorSerializationScheme : AbstractAMQPSerializationScheme(emptyList()) {
    override fun canDeserializeVersion(magic: CordaSerializationMagic, target: SerializationContext.UseCase): Boolean {
        return magic == amqpMagic
    }

    override fun rpcClientSerializerFactory(context: SerializationContext) = throw UnsupportedOperationException()
    override fun rpcServerSerializerFactory(context: SerializationContext) = throw UnsupportedOperationException()
}