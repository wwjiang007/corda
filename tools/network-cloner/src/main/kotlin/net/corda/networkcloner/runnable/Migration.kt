package net.corda.networkcloner.runnable

import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.api.Serializer
import net.corda.networkcloner.api.TxEditor
import net.corda.networkcloner.entity.MigrationTask
import net.corda.networkcloner.util.toTransactionComponents
import net.corda.node.services.persistence.DBTransactionStorage

class Migration(val migrationTask: MigrationTask, val serializer: Serializer, val txEditors : List<TxEditor>) : Runnable {

    override fun run() {
        val sourceMigrationData = migrationTask.sourceNodeDatabase.readMigrationData()

        val destinationTransactions = sourceMigrationData.transactions.map { sourceDbTransaction ->
            val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceDbTransaction.transaction)
            val sourceWireTransaction = sourceSignedTransaction.coreTransaction as WireTransaction
            val sourceTransactionComponents = sourceSignedTransaction.toTransactionComponents()

            val destTransactionComponents = txEditors.fold(sourceTransactionComponents) { tCs, txEditor -> txEditor.edit(tCs, migrationTask.migrationContext.identities) }
            val destComponentGroups = destTransactionComponents.toComponentGroups()

            val destWireTransaction = WireTransaction(destComponentGroups, sourceWireTransaction.privacySalt, sourceWireTransaction.digestService)
            val destSignedTransaction = SignedTransaction(destWireTransaction, sourceSignedTransaction.sigs)
            val destTxByteArray = serializer.serializeSignedTransaction(destSignedTransaction)
            with (sourceDbTransaction) {
                DBTransactionStorage.DBTransaction("xxx", stateMachineRunId, destTxByteArray, status, timestamp)
            }
        }

        val destMigrationData = sourceMigrationData.copy(transactions = destinationTransactions)

        migrationTask.destinationNodeDatabase.writeMigrationData(destMigrationData)
    }
}