package net.corda.networkcloner.test

import net.corda.core.cloning.MigrationContext
import net.corda.core.cloning.TxEditor
import net.corda.networkcloner.impl.IdentitySpaceImpl
import net.corda.networkcloner.impl.NodesToNodesMigrationTaskFactory
import net.corda.networkcloner.runnable.DefaultMigration
import net.corda.networkcloner.runnable.Migration
import org.junit.Ignore
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class MigrationTests : TestSupport() {

    @Test
    fun `Data copies from source to destination database`() {
        val snapshotDirectory = copyAndGetSnapshotDirectory("s1").second
        val sourceNodesDirectory = File(snapshotDirectory, "source")
        val destinationNodesDirectory = File(snapshotDirectory, "destination")

        val factory = NodesToNodesMigrationTaskFactory(sourceNodesDirectory, destinationNodesDirectory)
        val task = factory.getMigrationTasks().filter { it.sourceNodeDatabase.readMigrationData().transactions.size == 1 }.first()

        assertEquals(1, task.sourceNodeDatabase.readMigrationData().transactions.size)
        assertEquals(0, task.destinationNodeDatabase.readMigrationData().transactions.size)
        val noOpMigration = object : Migration(task, getSerializer(), getSigner(), false) {
            override fun getTxEditors(): List<TxEditor> = emptyList()
        }
        noOpMigration.run()
        val sourceMigrationData = task.sourceNodeDatabase.readMigrationData()
        val destinationMigrationData = task.destinationNodeDatabase.readMigrationData()
        assertEquals(1, sourceMigrationData.transactions.size)
        assertEquals(1, destinationMigrationData.transactions.size, "The transaction should have been copied from source to destination")
        assertEquals(2, sourceMigrationData.persistentParties.size)
        assertEquals(2, destinationMigrationData.persistentParties.size, "The persistent parties should have been copied from source to destination")
        assertEquals(1, sourceMigrationData.vaultLinearStates.size)
        assertEquals(1, destinationMigrationData.vaultLinearStates.size, "The vault linear states should have been copied from source to destination")
        assertEquals(1, sourceMigrationData.vaultStates.size)
        assertEquals(1, destinationMigrationData.vaultStates.size, "The vault states should have been copied from source to destination")
    }

    @Test
    fun `Data copies from source to destination database with all editors applied`() {
        val (snapshotDirectoryName,snapshotDirectory) = copyAndGetSnapshotDirectory("s1")
        val sourcePartyRepository = getPartyRepository(snapshotDirectoryName, "source")
        val destinationPartyRepository = getPartyRepository(snapshotDirectoryName, "destination")
        val identitySpace = IdentitySpaceImpl(sourcePartyRepository, destinationPartyRepository)
        val serializer = getSerializer()
        val sourceNodesDirectory = File(snapshotDirectory, "source")
        val destinationNodesDirectory = File(snapshotDirectory, "destination")

        val factory = NodesToNodesMigrationTaskFactory(sourceNodesDirectory, destinationNodesDirectory)
        val task = factory.getMigrationTasks().filter { it.sourceNodeDatabase.readMigrationData().transactions.size == 1 }.first()

        assertEquals(1, task.sourceNodeDatabase.readMigrationData().transactions.size)
        assertEquals(0, task.destinationNodeDatabase.readMigrationData().transactions.size)
        val cordappsRepository = getCordappsRepository()
        DefaultMigration(task, getSerializer(), getSigner(), cordappsRepository).run()
        val sourceMigrationData = task.sourceNodeDatabase.readMigrationData()
        val sourceNetworkParametersHash = task.sourceNodeDatabase.readNetworkParametersHash()
        val destinationMigrationData = task.destinationNodeDatabase.readMigrationData()
        val destinationNetworkParametersHash = task.destinationNodeDatabase.readNetworkParametersHash()
        assertEquals(1, sourceMigrationData.transactions.size)
        assertEquals(1, destinationMigrationData.transactions.size, "The transaction should have been copied from source to destination")
        verifyMigration(serializer, sourceMigrationData, destinationMigrationData, MigrationContext(identitySpace, sourceNetworkParametersHash, destinationNetworkParametersHash, emptyMap()))
    }

    @Test
    fun `Transactions with input states and reference states can migrate`() {
        val (snapshotDirectoryName,snapshotDirectory) = copyAndGetSnapshotDirectory("s3-input-states-and-ref-states")
        val sourcePartyRepository = getPartyRepository(snapshotDirectoryName, "source")
        val destinationPartyRepository = getPartyRepository(snapshotDirectoryName, "destination")
        val identitySpace = IdentitySpaceImpl(sourcePartyRepository, destinationPartyRepository)
        val serializer = getSerializer()
        val sourceNodesDirectory = File(snapshotDirectory, "source")
        val destinationNodesDirectory = File(snapshotDirectory, "destination")

        val factory = NodesToNodesMigrationTaskFactory(sourceNodesDirectory, destinationNodesDirectory)
        val migrationTasks = factory.getMigrationTasks()
        assertEquals(3, migrationTasks.size)
        val task = migrationTasks.filter { it.identity.sourceParty.name.toString().contains("client", true) }.single()

        assertEquals(3, task.sourceNodeDatabase.readMigrationData().transactions.size)
        assertEquals(0, task.destinationNodeDatabase.readMigrationData().transactions.size)
        val cordappsRepository = getCordappsRepository()
        DefaultMigration(task, getSerializer(), getSigner(), cordappsRepository).run()
        val sourceMigrationData = task.sourceNodeDatabase.readMigrationData()
        val sourceNetworkParametersHash = task.sourceNodeDatabase.readNetworkParametersHash()
        val destinationMigrationData = task.destinationNodeDatabase.readMigrationData()
        val destinationNetworkParametersHash = task.destinationNodeDatabase.readNetworkParametersHash()
        assertEquals(3, sourceMigrationData.transactions.size)
        assertEquals(3, destinationMigrationData.transactions.size, "The transaction should have been copied from source to destination")
        verifyMigration(serializer, sourceMigrationData, destinationMigrationData, MigrationContext(identitySpace, sourceNetworkParametersHash, destinationNetworkParametersHash, emptyMap()))
    }

}