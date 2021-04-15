package net.corda.networkcloner.test

import net.corda.core.internal.createComponentGroups
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.impl.SerializerImpl
import net.corda.networkcloner.impl.TransactionsStoreImpl
import org.junit.Test
import java.nio.file.Paths
import kotlin.test.assertTrue

class SerializerTests : TestSupport() {

    @Test
    fun `Transaction blob is identical when gone through deserialization and serialization`() {
        val pathToTestDb = SerializerTests::class.java.getResource("/snapshots/s1/source/persistence.mv.db").path.removeSuffix(".mv.db")
        val transactionsStore = TransactionsStoreImpl("jdbc:h2:$pathToTestDb","sa","")
        val sourceTxByteArray = transactionsStore.getAllTransactions().first()

        val serializer = getSerializer("s1")
        val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceTxByteArray)
        val sourceWireTransaction = sourceSignedTransaction.coreTransaction as WireTransaction

        val destComponentGroups = with(sourceWireTransaction) {
            createComponentGroups(inputs, outputs, commands, attachments, notary, timeWindow, references, networkParametersHash)
        }

        val destWireTransaction = WireTransaction(destComponentGroups, sourceWireTransaction.privacySalt, sourceWireTransaction.digestService)
        val destSignedTransaction = SignedTransaction(destWireTransaction, sourceSignedTransaction.sigs) //here obviously the sigs don't change
        val destTxByteArray = serializer.serializeSignedTransaction(destSignedTransaction)

        assertTrue(sourceTxByteArray.contentEquals(destTxByteArray))
    }

}