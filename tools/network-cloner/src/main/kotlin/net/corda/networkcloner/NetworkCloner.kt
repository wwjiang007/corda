package net.corda.networkcloner

import net.corda.core.internal.createComponentGroups
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.impl.CordappsRepositoryImpl
import net.corda.networkcloner.impl.SerializerImpl
import net.corda.networkcloner.impl.SignerImpl
import net.corda.networkcloner.impl.NodeDatabaseImpl
import net.corda.serialization.internal.CordaSerializationEncoding
import java.io.File
import java.nio.file.Paths

fun main(args: Array<String>) {

    println("Hello there")

    val cordappLoader = CordappsRepositoryImpl(File("/Users/alex.koller/Projects/contract-sdk/examples/test-app/buildDestination/nodes/Operator/cordapps"))
    val serializer = SerializerImpl(cordappLoader.getCordappLoader())
    val transactionStore = NodeDatabaseImpl("jdbc:h2:/Users/alex.koller/Projects/contract-sdk/examples/test-app/buildSource/nodes/Operator/persistence", "sa", "", { _ -> throw UnsupportedOperationException()}, { _ -> throw java.lang.UnsupportedOperationException()})
    val migrationData = transactionStore.readMigrationData()
    val signer = SignerImpl()


    migrationData.transactions.forEach {
        val deserialized = serializer.deserializeDbBlobIntoTransaction(it.transaction)

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


        val sTx = SignedTransaction(destinationWireTransaction, deserialized.sigs)

        val serializedFromSignedTx = sTx.serialize(context = SerializationDefaults.STORAGE_CONTEXT.withEncoding(CordaSerializationEncoding.SNAPPY))

        println("Db tx and destination tx are same: ${it.transaction.contentEquals(serializedFromSignedTx.bytes)}")

    }

    println("Done")


}