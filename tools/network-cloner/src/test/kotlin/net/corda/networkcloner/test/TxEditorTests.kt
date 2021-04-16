package net.corda.networkcloner.test

import net.corda.core.internal.createComponentGroups
import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.entity.TransactionComponents
import net.corda.networkcloner.impl.TransactionsStoreImpl
import net.corda.networkcloner.test.txeditors.TestAppTxEditor
import org.junit.Test

class TxEditorTests : TestSupport() {

    @Test
    fun `An editor can be invoked on a transaction`() {
        val pathToTestDb = TxEditorTests::class.java.getResource("/snapshots/s1/source/persistence.mv.db").path.removeSuffix(".mv.db")
        val transactionsStore = TransactionsStoreImpl("jdbc:h2:$pathToTestDb","sa","")
        val sourceTxByteArray = transactionsStore.getAllTransactions().first()

        val serializer = getSerializer("s1")
        val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceTxByteArray)
        val sourceWireTransaction = sourceSignedTransaction.coreTransaction as WireTransaction

        val identityMapper = getIdentityMapper("s1")
        /*
        val clientSourceIdentity = identityMapper.getSourceIdentity(clientX500Name) ?: throw AssertionError("Expected to find identity $clientX500Name")
        val clientDestinationIdentity = identityMapper.getDestinationIdentity(clientX500Name) ?: throw AssertionError("Expected to find identity $clientX500Name")

        val txEditor = TestAppTxEditor()
        val transactionComponents = with(sourceWireTransaction) {
            TransactionComponents(inputs, outputs, commands, attachments, notary, timeWindow, references, networkParametersHash)
        }
        val editedTransactionComponents = txEditor.edit(transactionComponents, mapOf(clientSourceIdentity to clientDestinationIdentity))
        
         */

    }

}