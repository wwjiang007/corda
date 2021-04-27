package net.corda.networkcloner.test

import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.entity.TransactionComponents
import net.corda.networkcloner.impl.NodeDatabaseImpl
import net.corda.networkcloner.test.txeditors.TestAppTxEditor
import org.junit.Ignore
import org.junit.Test

class TxEditorTests : TestSupport() {

    @Test
    @Ignore //this is not very clear yet how this should work
    fun `An editor can be invoked on a transaction`() {
        val pathToTestDb = TxEditorTests::class.java.getResource("/snapshots/s1/source/persistence.mv.db").path.removeSuffix(".mv.db")
        val transactionsStore = NodeDatabaseImpl("jdbc:h2:$pathToTestDb","sa","")
        val sourceTxByteArray = transactionsStore.readMigrationData().transactions.first().transaction

        val serializer = getSerializer("s1")
        val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceTxByteArray)
        val sourceWireTransaction = sourceSignedTransaction.coreTransaction as WireTransaction

        //val identityMapper = getIdentityMapper("s1")
        //val identities = identityMapper.getAllIdentities()

        val txEditor = TestAppTxEditor()
        val transactionComponents = with(sourceWireTransaction) {
            TransactionComponents(inputs, outputs, commands, attachments, notary, timeWindow, references, networkParametersHash)
        }
        //val editedTransactionComponents = txEditor.edit(transactionComponents, identities)



    }

}