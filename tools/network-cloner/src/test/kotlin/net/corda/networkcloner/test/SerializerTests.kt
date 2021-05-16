package net.corda.networkcloner.test

import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.impl.NodeDatabaseImpl
import net.corda.networkcloner.util.toTransactionComponents
import org.junit.Test
import kotlin.test.assertTrue

class SerializerTests : TestSupport() {

    @Test
    fun `Transaction blob is identical when gone through deserialization and serialization`() {
        val pathToTestDb = SerializerTests::class.java.getResource("/snapshots/s1/source/client/persistence.mv.db").path.removeSuffix(".mv.db")
        val transactionsStore = NodeDatabaseImpl("jdbc:h2:$pathToTestDb","sa","", {_ -> null}, { _ -> null}, emptyList(), emptyList(), emptyList())
        val sourceTxByteArray = transactionsStore.readCoreCordaData().transactions.first().transaction

        val serializer = getSerializer()
        val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceTxByteArray)
        val sourceWireTransaction = sourceSignedTransaction.coreTransaction as WireTransaction

        val destComponentGroups = sourceSignedTransaction.toTransactionComponents().toComponentGroups()

        val destWireTransaction = WireTransaction(destComponentGroups, sourceWireTransaction.privacySalt, sourceWireTransaction.digestService)
        val destSignedTransaction = SignedTransaction(destWireTransaction, sourceSignedTransaction.sigs) //here obviously the sigs don't change
        val destTxByteArray = serializer.serializeSignedTransaction(destSignedTransaction)

        assertTrue(sourceTxByteArray.contentEquals(destTxByteArray))
    }

}