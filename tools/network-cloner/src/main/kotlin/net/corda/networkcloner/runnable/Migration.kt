package net.corda.networkcloner.runnable

import net.corda.core.cloning.IdentitySpace
import net.corda.core.cloning.MigrationContext
import net.corda.core.cloning.TxEditor
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.TransactionSignature
import net.corda.core.identity.Party
import net.corda.core.schemas.PersistentStateRef
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.FailedAssumptionException
import net.corda.networkcloner.NoDestinationTransactionFoundException
import net.corda.networkcloner.api.Serializer
import net.corda.networkcloner.api.Signer
import net.corda.networkcloner.entity.MigrationData
import net.corda.networkcloner.entity.MigrationTask
import net.corda.networkcloner.util.toTransactionComponents
import net.corda.node.services.persistence.DBTransactionStorage
import net.corda.node.services.vault.VaultSchemaV1

abstract class Migration(val migrationTask: MigrationTask, val serializer: Serializer, val signer: Signer, val dryRun: Boolean) : Runnable {

    override fun run() {
        println("Executing migration task $migrationTask")
        val sourceMigrationData = migrationTask.sourceNodeDatabase.readMigrationData()
        val sourceTransactions = sourceMigrationData.transactions.map {
            val signedTransaction = serializer.deserializeDbBlobIntoTransaction(it.transaction)
            SourceTransaction(it, signedTransaction)
        }

        val destinationTransactions = getDestinationTransactions(sourceTransactions, mutableMapOf())
        val destinationWireTransactions = destinationTransactions.map { it.wireTransaction }
        val destinationDbTransactions = destinationTransactions.map { it.dbTransaction }
        val destinationStatePartyMapping = getDestinationStatePartyMapping(destinationWireTransactions)
        val destinationVaultLinearStates = getDestinationVaultLinearStates(destinationWireTransactions)
        val destinationVaultStates = getDestinationVaultStates(sourceMigrationData.vaultStates, destinationTransactions, migrationTask.migrationContext.identitySpace)

        val destMigrationData = sourceMigrationData.copy(transactions = destinationDbTransactions,
                persistentParties = destinationStatePartyMapping,
                vaultLinearStates = destinationVaultLinearStates,
                vaultStates = destinationVaultStates)

        if (dryRun) {
            println("This is a dry run for ${migrationTask.identity.sourceParty}, not writing migration data to destination database")
        } else {
            println("Writing migrated data to dataase of party ${migrationTask.identity.sourceParty}")
            migrationTask.destinationNodeDatabase.writeMigrationData(destMigrationData)
        }
    }

    private fun getDestinationVaultLinearStates(destinationTransactions: Collection<WireTransaction>): List<VaultSchemaV1.VaultLinearStates> {
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

    private fun getDestinationStatePartyMapping(destinationTransactions: Collection<WireTransaction>): List<VaultSchemaV1.PersistentParty> {
        return destinationTransactions.flatMap { transaction ->
            transaction.outputs.mapIndexed { outputIndex, transactionOutputState ->
                transactionOutputState.data.participants.map { participant ->
                    val persistentStateRef = PersistentStateRef(StateRef(transaction.id, outputIndex))
                    VaultSchemaV1.PersistentParty(persistentStateRef, participant)
                }
            }.flatten()
        }
    }

    private fun getDestinationTransactions(sourceTransactions : List<SourceTransaction>, sourceToDestTxId : MutableMap<SecureHash, SecureHash>): List<DestinationTransaction> {
        println("Migrating ${sourceTransactions.size} source transactions")
        val leftOverSourceTransactions = mutableListOf<SourceTransaction>()
        val destinationTransactions = sourceTransactions.mapNotNull { sourceTransaction ->
            if (readyForMigration(sourceTransaction, sourceToDestTxId)) {
                createDestinationTransaction(sourceTransaction, sourceToDestTxId).also {
                    sourceToDestTxId.put(sourceTransaction.signedTransaction.id, it.wireTransaction.id)
                }
            } else {
                leftOverSourceTransactions.add(sourceTransaction)
                null
            }
        }
        return if (leftOverSourceTransactions.isEmpty()) {
            destinationTransactions
        } else {
            destinationTransactions + getDestinationTransactions(leftOverSourceTransactions, sourceToDestTxId)
        }
    }

    private fun createDestinationTransaction(sourceTransaction : SourceTransaction, sourceToDestTxId : Map<SecureHash, SecureHash>) : DestinationTransaction {
        val sourceDbTransaction = sourceTransaction.dbTransaction
        val sourceSignedTransaction = sourceTransaction.signedTransaction
        val sourceWireTransaction = sourceSignedTransaction.coreTransaction as WireTransaction
        val sourceTransactionComponents = sourceSignedTransaction.toTransactionComponents()

        val migrationContext = migrationTask.migrationContext.copy(sourceTxIdToDestTxId = sourceToDestTxId)
        val destTransactionComponents = getTxEditors().fold(sourceTransactionComponents) { tCs, txEditor -> txEditor.edit(tCs, migrationContext) }
        val destComponentGroups = destTransactionComponents.toComponentGroups()

        val destWireTransaction = WireTransaction(destComponentGroups, sourceWireTransaction.privacySalt, sourceWireTransaction.digestService)
        val newSignatures = getSignatures(destWireTransaction.id, sourceSignedTransaction.sigs, migrationTask.migrationContext)
        val destSignedTransaction = SignedTransaction(destWireTransaction, newSignatures)
        val destTxByteArray = serializer.serializeSignedTransaction(destSignedTransaction)
        val dbTransaction = with(sourceDbTransaction) {
            DBTransactionStorage.DBTransaction(destWireTransaction.id.toString(), stateMachineRunId, destTxByteArray, status, timestamp)
        }
        return DestinationTransaction(destWireTransaction, dbTransaction, sourceDbTransaction.txId)
    }

    private fun getDestinationVaultStates(sourceVaultStates: List<VaultSchemaV1.VaultStates>, destinationTransactions: List<DestinationTransaction>, identitySpace: IdentitySpace): List<VaultSchemaV1.VaultStates> {
        return sourceVaultStates.map { sourceVaultState ->
            val destinationNotary = identitySpace.findDestinationForSourceParty(sourceVaultState.notary) as Party
            val destinationVaultState = with(sourceVaultState) {
                VaultSchemaV1.VaultStates(
                        destinationNotary,
                        contractStateClassName,
                        stateStatus,
                        recordedTime,
                        consumedTime,
                        lockId,
                        relevancyStatus,
                        lockUpdateTime,
                        constraintType,
                        constraintData)
            }
            val sourceOutputStateRef = sourceVaultState.stateRef ?: throw FailedAssumptionException("Expected to find stateRef on the source vault state $sourceVaultState")
            val sourceTransactionId = sourceOutputStateRef.txId
            val sourceOutputStateIndex = sourceOutputStateRef.index
            val destinationTransaction = destinationTransactions.find { it.sourceTransactionId == sourceTransactionId } ?: throw NoDestinationTransactionFoundException(sourceTransactionId)
            destinationVaultState.stateRef = PersistentStateRef(destinationTransaction.dbTransaction.txId, sourceOutputStateIndex)
            destinationVaultState
        }
    }

    private fun getSignatures(transactionId: SecureHash, originalSigners: List<TransactionSignature>, migrationContext: MigrationContext): List<TransactionSignature> {
        val newSigners = originalSigners.map { migrationContext.identitySpace.getDestinationPartyAndPrivateKey(it.by).keyPair }
        return signer.sign(transactionId, newSigners)
    }

    private fun readyForMigration(sourceTransaction : SourceTransaction, sourceToDestTxId: Map<SecureHash, SecureHash>) : Boolean {
        val stateRefsToCheck = sourceTransaction.signedTransaction.inputs + sourceTransaction.signedTransaction.references
        return stateRefsToCheck.all {
            sourceToDestTxId.containsKey(it.txhash)
        }
    }

    abstract fun getTxEditors(): List<TxEditor>

    private data class SourceTransaction(val dbTransaction: DBTransactionStorage.DBTransaction, val signedTransaction: SignedTransaction)
    private data class DestinationTransaction(val wireTransaction: WireTransaction, val dbTransaction: DBTransactionStorage.DBTransaction, val sourceTransactionId: String)
}