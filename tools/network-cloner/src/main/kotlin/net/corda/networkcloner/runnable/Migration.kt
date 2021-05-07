package net.corda.networkcloner.runnable

import net.corda.core.cloning.MigrationContext
import net.corda.core.cloning.TxEditor
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.TransactionSignature
import net.corda.core.schemas.PersistentStateRef
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.api.Serializer
import net.corda.networkcloner.api.Signer
import net.corda.networkcloner.entity.MigrationData
import net.corda.networkcloner.entity.MigrationTask
import net.corda.networkcloner.util.toTransactionComponents
import net.corda.node.services.persistence.DBTransactionStorage
import net.corda.node.services.vault.VaultSchemaV1

abstract class Migration(val migrationTask: MigrationTask, val serializer: Serializer, val signer : Signer) : Runnable {

    override fun run() {
        val sourceMigrationData = migrationTask.sourceNodeDatabase.readMigrationData()

        val destinationTransactions = getDestinationTransactions(sourceMigrationData)
        val destinationStatePartyMapping = getDestinationStatePartyMapping(destinationTransactions.keys)
        val destinationVaultLinearStates = getDestinationVaultLinearStates(destinationTransactions.keys)

        val destMigrationData = sourceMigrationData.copy(transactions = destinationTransactions.values.toList(),
                                                         persistentParties = destinationStatePartyMapping,
                                                         vaultLinearStates = destinationVaultLinearStates)

        migrationTask.destinationNodeDatabase.writeMigrationData(destMigrationData)
    }

    private fun getDestinationVaultLinearStates(destinationTransactions: Collection<WireTransaction>) : List<VaultSchemaV1.VaultLinearStates> {
        return destinationTransactions.flatMap { transaction ->
            transaction.outputs.mapIndexedNotNull { outputIndex, transactionOutputState ->
                val outputState = transactionOutputState.data
                if (outputState is LinearState) {
                    val persistentStateRef = PersistentStateRef(StateRef(transaction.id, outputIndex))
                    VaultSchemaV1.VaultLinearStates(outputState.linearId.externalId, outputState.linearId.id).apply {
                        stateRef = persistentStateRef
                    }
                } else {
                    null
                }
            }
        }
    }

    private fun getDestinationStatePartyMapping(destinationTransactions : Collection<WireTransaction>) : List<VaultSchemaV1.PersistentParty> {
        return destinationTransactions.flatMap { transaction ->
            transaction.outputs.mapIndexed { outputIndex, transactionOutputState ->
                transactionOutputState.data.participants.map { participant ->
                    val persistentStateRef = PersistentStateRef(StateRef(transaction.id, outputIndex))
                    VaultSchemaV1.PersistentParty(persistentStateRef, participant)
                }
            }.flatten()
        }
    }

    private fun getDestinationTransactions(sourceMigrationData : MigrationData) : Map<WireTransaction, DBTransactionStorage.DBTransaction> {
        return sourceMigrationData.transactions.map { sourceDbTransaction ->
            val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceDbTransaction.transaction)
            val sourceWireTransaction = sourceSignedTransaction.coreTransaction as WireTransaction
            val sourceTransactionComponents = sourceSignedTransaction.toTransactionComponents()

            val destTransactionComponents = getTxEditors().fold(sourceTransactionComponents) { tCs, txEditor -> txEditor.edit(tCs, migrationTask.migrationContext) }
            val destComponentGroups = destTransactionComponents.toComponentGroups()

            val destWireTransaction = WireTransaction(destComponentGroups, sourceWireTransaction.privacySalt, sourceWireTransaction.digestService)
            val newSignatures = getSignatures(destWireTransaction.id, sourceSignedTransaction.sigs, migrationTask.migrationContext)
            val destSignedTransaction = SignedTransaction(destWireTransaction, newSignatures)
            val destTxByteArray = serializer.serializeSignedTransaction(destSignedTransaction)
            destWireTransaction to with (sourceDbTransaction) {
                DBTransactionStorage.DBTransaction(destWireTransaction.id.toString(), stateMachineRunId, destTxByteArray, status, timestamp)
            }
        }.toMap()
    }

    private fun getSignatures(transactionId : SecureHash, originalSigners : List<TransactionSignature>, migrationContext: MigrationContext) : List<TransactionSignature> {
        val newSigners = originalSigners.map { migrationContext.identitySpace.getDestinationPartyAndPrivateKey(it.by).keyPair }
        return signer.sign(transactionId, newSigners)
    }

    abstract fun getTxEditors() : List<TxEditor>
}