package net.corda.networkcloner

import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.internal.SerializationEnvironment
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.networkcloner.impl.TransactionsStoreImpl
import net.corda.serialization.internal.AMQP_P2P_CONTEXT
import net.corda.serialization.internal.AMQP_STORAGE_CONTEXT
import net.corda.serialization.internal.CordaSerializationEncoding
import net.corda.serialization.internal.CordaSerializationMagic
import net.corda.serialization.internal.SerializationFactoryImpl
import net.corda.serialization.internal.amqp.AbstractAMQPSerializationScheme
import net.corda.serialization.internal.amqp.amqpMagic

fun main(args: Array<String>) {

    println("Hello there")

    val transactionStore = TransactionsStoreImpl()
    val transactions = transactionStore.getAllTransactions()

    initialiseSerialization()

    transactions.forEach {
        val deserialized = it.deserialize<Any>(context = SerializationDefaults.STORAGE_CONTEXT)




        val serializedOne = deserialized.serialize(context = SerializationDefaults.STORAGE_CONTEXT.withEncoding(CordaSerializationEncoding.SNAPPY))
        val serializedTwo = deserialized.serialize(context = SerializationDefaults.STORAGE_CONTEXT.withEncoding(CordaSerializationEncoding.SNAPPY))



        println("are identical? ${it.contentEquals(serializedOne.bytes)}")
        println("are identical? ${serializedTwo.bytes.contentEquals(serializedOne.bytes)}")
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