package net.corda.networkcloner.runnable

import net.corda.core.cloning.MigrationContext
import net.corda.core.cloning.TxEditor
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.TransactionSignature
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.api.Serializer
import net.corda.networkcloner.api.Signer
import net.corda.networkcloner.entity.MigrationTask
import net.corda.networkcloner.util.toTransactionComponents
import net.corda.node.services.persistence.DBTransactionStorage

abstract class Migration(val migrationTask: MigrationTask, val serializer: Serializer, val signer : Signer) : Runnable {

    override fun run() {
        val sourceMigrationData = migrationTask.sourceNodeDatabase.readMigrationData()

        val destinationTransactions = sourceMigrationData.transactions.map { sourceDbTransaction ->
            val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceDbTransaction.transaction)
            val sourceWireTransaction = sourceSignedTransaction.coreTransaction as WireTransaction
            val sourceTransactionComponents = sourceSignedTransaction.toTransactionComponents()

            val destTransactionComponents = getTxEditors().fold(sourceTransactionComponents) { tCs, txEditor -> txEditor.edit(tCs, migrationTask.migrationContext) }
            val destComponentGroups = destTransactionComponents.toComponentGroups()

            val destWireTransaction = WireTransaction(destComponentGroups, sourceWireTransaction.privacySalt, sourceWireTransaction.digestService)
            val newSignatures = getSignatures(destWireTransaction.id, sourceSignedTransaction.sigs, migrationTask.migrationContext)
            val destSignedTransaction = SignedTransaction(destWireTransaction, newSignatures)
            val destTxByteArray = serializer.serializeSignedTransaction(destSignedTransaction)
            with (sourceDbTransaction) {
                DBTransactionStorage.DBTransaction(destWireTransaction.id.toString(), stateMachineRunId, destTxByteArray, status, timestamp)
            }
        }

        val destMigrationData = sourceMigrationData.copy(transactions = destinationTransactions)

        migrationTask.destinationNodeDatabase.writeMigrationData(destMigrationData)
    }

    private fun getSignatures(transactionId : SecureHash, originalSigners : List<TransactionSignature>, migrationContext: MigrationContext) : List<TransactionSignature> {
        val newSigners = originalSigners.map { migrationContext.getDestinationPartyAndPrivateKey(it.by).keyPair }
        return signer.sign(transactionId, newSigners)
    }

    abstract fun getTxEditors() : List<TxEditor>
}