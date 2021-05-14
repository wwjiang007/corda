package net.corda.networkcloner.test

import net.corda.core.cloning.MigrationContext
import net.corda.core.cloning.TransactionComponents
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.toPath
import net.corda.networkcloner.api.CordappsRepository
import net.corda.networkcloner.api.NodeDatabase
import net.corda.networkcloner.api.PartyRepository
import net.corda.networkcloner.api.Serializer
import net.corda.networkcloner.api.Signer
import net.corda.networkcloner.entity.CoreCordaData
import net.corda.networkcloner.impl.CordappsRepositoryImpl
import net.corda.networkcloner.impl.NodeDatabaseImpl
import net.corda.networkcloner.impl.NodesDirPartyRepository
import net.corda.networkcloner.impl.SerializerImpl
import net.corda.networkcloner.impl.SignerImpl
import net.corda.networkcloner.util.toTransactionComponents
import org.junit.Assert.assertTrue
import java.io.File
import java.util.*
import kotlin.test.assertEquals

open class TestSupport {

    val clientX500Name = CordaX500Name.parse("O=Client,L=London,C=GB")
    val operatorX500Name = CordaX500Name.parse("O=Operator,L=New York,C=US")
    val notaryX500Name = CordaX500Name.parse("O=Notary,L=London,C=GB")

    fun getSigner() : Signer {
        return SignerImpl()
    }

    fun getSerializer() : Serializer {
        return if (serializer == null) {
            val cordappLoader = getCordappsRepository().getCordappLoader()
            SerializerImpl(cordappLoader).also {
                serializer = it
            }
        } else {
            serializer!!
        }
    }

    private fun getSnapshotsDirectory() : File {
        return TestSupport::class.java.getResource("/snapshots").toPath().toFile()
    }

    fun getPartyRepository(snapshot : String, sourceOrDestination: String) : PartyRepository {
        val nodesDir = File(getSnapshotDirectory(snapshot), sourceOrDestination)
        return NodesDirPartyRepository(nodesDir)
    }

    fun getNodeDatabase(snapshot: String, sourceOrDestination: String, node: String, wellKnownPartyFromX500Name: (CordaX500Name) -> Party? = {_ -> null}, wellKnownPartyFromAnonymous: (AbstractParty) -> Party? = {_ -> null}, additionalMappedClasses : List<Class<*>> = emptyList()) : NodeDatabase {
        val pathToDbFileWithoutSuffix = TxEditorTests::class.java.getResource("/snapshots/$snapshot/$sourceOrDestination/$node/persistence.mv.db").path.removeSuffix(".mv.db")
        return NodeDatabaseImpl("jdbc:h2:$pathToDbFileWithoutSuffix","sa","", wellKnownPartyFromX500Name, wellKnownPartyFromAnonymous, additionalMappedClasses)
    }

    fun getCordappsRepository() : CordappsRepository {
        return if (cordappsRepository == null) {
            val pathToCordapps = File(getSnapshotsDirectory(),"tx-editor-plugins")
            CordappsRepositoryImpl(pathToCordapps, 1, 0).also {
                cordappsRepository = it
            }
        } else {
            cordappsRepository!!
        }
    }

    fun getSnapshotDirectory(snapshot: String) : File {
        return File(getSnapshotsDirectory(), snapshot)
    }

    fun copyAndGetSnapshotDirectory(snapshot: String) : Pair<String,File> {
        val snapshotsDirectory = getSnapshotsDirectory()
        val snapshotDirectory = getSnapshotDirectory(snapshot)
        val snapshotDirectoryName = UUID.randomUUID().toString()
        val copyDirectory = File(snapshotsDirectory, snapshotDirectoryName)
        snapshotDirectory.copyRecursively(copyDirectory)
        return snapshotDirectoryName to copyDirectory
    }

    fun verifyMigration(serializer: Serializer, sourceData : CoreCordaData, destinationData : CoreCordaData, context : MigrationContext) {
        val sourceTransactions = sourceData.transactions.map { serializer.deserializeDbBlobIntoTransaction(it.transaction).toTransactionComponents() }
        val destinationTransactions = destinationData.transactions.map { serializer.deserializeDbBlobIntoTransaction(it.transaction).toTransactionComponents() }
        verifyTransactions(sourceTransactions, destinationTransactions, context)
        verifyPersistentParties(destinationData)
        verifyVaultLinearStates(destinationData)
        verifyVaultStates(destinationData)
    }

    fun verifyTransactions(sourceTransactions : List<TransactionComponents>, destinationTransactions : List<TransactionComponents>, context: MigrationContext) {
        assertEquals(sourceTransactions.size, destinationTransactions.size)
        val sourceTxHashToDestTxHash = sourceTransactions.map { it.txId }.zip(destinationTransactions.map { it.txId }).toMap()
        sourceTransactions.forEachIndexed { index, sourceTransaction ->
            val destinationTransaction = destinationTransactions[index]
            assertEquals(sourceTransaction.outputs.size, destinationTransaction.outputs.size)
            sourceTransaction.outputs.forEachIndexed { outputIndex, outputState ->
                val sourceParticipants = outputState.data.participants
                val expectedParticipants = sourceParticipants.map { context.identitySpace.findDestinationForSourceParty(it) }
                assertEquals(expectedParticipants, destinationTransaction.outputs[outputIndex].data.participants, "Destination output state participants are not as expected")
                val expectedNotary = context.identitySpace.findDestinationForSourceParty(outputState.notary)
                assertEquals(expectedNotary, destinationTransaction.outputs[outputIndex].notary, "Destination output state notary not as expected")
            }
            val expectedNotary = context.identitySpace.findDestinationForSourceParty(sourceTransaction.notary!!) as Party
            assertEquals(expectedNotary, destinationTransaction.notary, "Destination notary is not as expected")

            assertEquals(sourceTransaction.commands.size, destinationTransaction.commands.size)
            sourceTransaction.commands.forEachIndexed { commandIndex, command ->
                val sourceSigners = command.signers
                val expectedSigners = sourceSigners.map { context.identitySpace.findDestinationForSourceOwningKey(it) }
                assertEquals(expectedSigners, destinationTransaction.commands[commandIndex].signers, "Destination command signers are not as expected")
            }
            assertEquals(sourceTransaction.inputs.size, destinationTransaction.inputs.size)
            sourceTransaction.inputs.forEachIndexed { inputIndex, inputState ->
                val expectedDestInputStateTxId = sourceTxHashToDestTxHash[inputState.txhash]
                assertEquals(expectedDestInputStateTxId, destinationTransaction.inputs[inputIndex].txhash)
            }
            assertEquals(sourceTransaction.references.size, destinationTransaction.references.size)
            sourceTransaction.references.forEachIndexed { referenceIndex, referenceState ->
                val expectedDestReferenceStateTxId = sourceTxHashToDestTxHash[referenceState.txhash]
                assertEquals(expectedDestReferenceStateTxId, destinationTransaction.references[referenceIndex].txhash)
            }
        }

        destinationTransactions.forEach {
            assertEquals(context.destinationNetworkParametersHash, it.networkParametersHash, "Destination network parameter hash is not as expected")
        }
    }

    fun verifyPersistentParties(destinationData: CoreCordaData) {
        //check that all transactions are referenced from the persistent parties table
        assertTrue(destinationData.transactions.all {
            destinationData.persistentParties.find { persistentParty ->  persistentParty.compositeKey.stateRef?.txId == it.txId } != null
        })
    }

    fun verifyVaultLinearStates(destinationData: CoreCordaData) {
        //check that all transactions are referenced from the vault linear states table
        assertTrue(destinationData.transactions.all {
            destinationData.vaultLinearStates.find { vaultLinearState ->  vaultLinearState.stateRef?.txId == it.txId } != null
        })
    }

    fun verifyVaultStates(destinationData: CoreCordaData) {
        //check that all transactions are referenced from the vault states table
        assertTrue(destinationData.transactions.all {
            destinationData.vaultStates.find { vaultState ->  vaultState.stateRef?.txId == it.txId } != null
        })
    }

    companion object {
        var serializer : Serializer? = null
        var cordappsRepository : CordappsRepository? = null
    }

}