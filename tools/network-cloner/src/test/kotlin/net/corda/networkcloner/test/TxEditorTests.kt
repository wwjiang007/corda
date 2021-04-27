package net.corda.networkcloner.test

import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.entity.TransactionComponents
import net.corda.networkcloner.impl.NodeDatabaseImpl
import net.corda.networkcloner.impl.NodesDirPartyRepository
import net.corda.networkcloner.test.txeditors.TestAppTxEditor
import org.junit.Ignore
import org.junit.Test

class TxEditorTests : TestSupport() {

    @Test
    fun `Party replacing editor works`() {
        val nodeDatabase = getNodeDatabase("s2","source","client")
        val sourceTxByteArray = nodeDatabase.readMigrationData().transactions.first().transaction

        val serializer = getSerializer("s2")
        val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceTxByteArray)
        val sourceWireTransaction = sourceSignedTransaction.coreTransaction as WireTransaction

        val sourcePartyRepository = getPartyRepository("s2","source")
        //val identities = identityMapper.getAllIdentities()

        val txEditor = TestAppTxEditor()
        val transactionComponents = with(sourceWireTransaction) {
            TransactionComponents(inputs, outputs, commands, attachments, notary, timeWindow, references, networkParametersHash)
        }
        //val editedTransactionComponents = txEditor.edit(transactionComponents, identities)



    }

}