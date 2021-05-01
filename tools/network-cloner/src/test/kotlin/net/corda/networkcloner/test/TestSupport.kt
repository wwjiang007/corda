package net.corda.networkcloner.test

import net.corda.core.cloning.MigrationContext
import net.corda.core.cloning.TransactionComponents
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.toPath
import net.corda.core.transactions.SignedTransaction
import net.corda.networkcloner.api.CordappsRepository
import net.corda.networkcloner.api.NodeDatabase
import net.corda.networkcloner.api.PartyRepository
import net.corda.networkcloner.api.Serializer
import net.corda.networkcloner.entity.MigrationData
import net.corda.networkcloner.impl.CordappsRepositoryImpl
import net.corda.networkcloner.impl.NodeDatabaseImpl
import net.corda.networkcloner.impl.NodesDirPartyRepository
import net.corda.networkcloner.impl.SerializerImpl
import net.corda.networkcloner.util.toTransactionComponents
import java.io.File
import java.util.*
import kotlin.test.assertEquals

open class TestSupport {

    val clientX500Name = CordaX500Name.parse("O=Client,L=London,C=GB")
    val operatorX500Name = CordaX500Name.parse("O=Operator,L=New York,C=US")

    //@todo this storing to a static property doesn't really work if different tests ask for different snapshot
    fun getSerializer(snapshot: String) : Serializer {
        return if (serializer == null) {
            val cordappLoader = getCordappsRepository(snapshot).getCordappLoader()
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

    fun getNodeDatabase(snapshot: String, sourceOrDestination: String, node: String) : NodeDatabase {
        val pathToDbFileWithoutSuffix = TxEditorTests::class.java.getResource("/snapshots/$snapshot/$sourceOrDestination/$node/persistence.mv.db").path.removeSuffix(".mv.db")
        return NodeDatabaseImpl("jdbc:h2:$pathToDbFileWithoutSuffix","sa","")
    }

    fun getCordappsRepository(snapshot: String) : CordappsRepository {
        return if (cordappsRepository == null) {
            val pathToCordapps = File(getSnapshotDirectory(snapshot),"tx-editor-plugins")
            CordappsRepositoryImpl(pathToCordapps).also {
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

    fun verifyMigration(serializer: Serializer, sourceData : MigrationData, destinationData : MigrationData, context : MigrationContext) {
        val sourceTransactions = sourceData.transactions.map { serializer.deserializeDbBlobIntoTransaction(it.transaction).toTransactionComponents() }
        val destinationTransactions = destinationData.transactions.map { serializer.deserializeDbBlobIntoTransaction(it.transaction).toTransactionComponents() }
        verifyTransactions(sourceTransactions, destinationTransactions, context)
    }

    fun verifyTransactions(sourceTransactions : List<TransactionComponents>, destinationTransactions : List<TransactionComponents>, context: MigrationContext) {
        assertEquals(sourceTransactions.size, destinationTransactions.size)
        sourceTransactions.forEachIndexed { index, sourceTransaction ->
            val destinationTransaction = destinationTransactions[index]
            assertEquals(sourceTransaction.outputs.size, destinationTransaction.outputs.size)
            sourceTransaction.outputs.forEachIndexed { outputIndex, outputState ->
                val sourceParticipants = outputState.data.participants
                val expectedParticipants = sourceParticipants.map { context.findDestinationForSourceParty(it) }
                assertEquals(expectedParticipants, destinationTransaction.outputs[outputIndex].data.participants, "Destination output state participants are not as expected")
            }
            val expectedNotary = context.findDestinationForSourceParty(sourceTransaction.notary!!) as Party
            assertEquals(expectedNotary, destinationTransaction.notary, "Destination notary is not as expected")

            assertEquals(sourceTransaction.commands.size, destinationTransaction.commands.size)
            sourceTransaction.commands.forEachIndexed { commandIndex, command ->
                val sourceSigners = command.signers
                val expectedSigners = sourceSigners.map { context.findDestinationForSourceOwningKey(it) }
                assertEquals(expectedSigners, destinationTransaction.commands[commandIndex].signers, "Destination command signers are not as expected")
            }
        }

        destinationTransactions.forEach {
            assertEquals(context.destinationNetworkParametersHash, it.networkParametersHash, "Destination network parameter hash is not as expected")
        }
    }

    companion object {
        var serializer : Serializer? = null
        var cordappsRepository : CordappsRepository? = null
    }

}