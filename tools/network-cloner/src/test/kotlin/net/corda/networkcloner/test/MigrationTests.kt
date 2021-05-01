package net.corda.networkcloner.test

import net.corda.core.cloning.MigrationContext
import net.corda.core.cloning.TxEditor
import net.corda.networkcloner.impl.NodesToNodesMigrationTaskFactory
import net.corda.networkcloner.impl.SerializerImpl
import net.corda.networkcloner.runnable.DefaultMigration
import net.corda.networkcloner.runnable.Migration
import net.corda.networkcloner.util.IdentityFactory
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class MigrationTests : TestSupport() {

    @Test
    fun `Data copies from source to destination database`() {
        val snapshotDirectory = copyAndGetSnapshotDirectory("s2").second
        val sourceNodesDirectory = File(snapshotDirectory, "source")
        val destinationNodesDirectory = File(snapshotDirectory, "destination")

        val factory = NodesToNodesMigrationTaskFactory(sourceNodesDirectory, destinationNodesDirectory)
        val task = factory.getMigrationTasks().single()

        assertEquals(1, task.sourceNodeDatabase.readMigrationData().transactions.size)
        assertEquals(0, task.destinationNodeDatabase.readMigrationData().transactions.size)
        val noOpMigration = object : Migration(task, getSerializer("s2")) {
            override fun getTxEditors(): List<TxEditor> = emptyList()
        }
        noOpMigration.run()
        assertEquals(1, task.sourceNodeDatabase.readMigrationData().transactions.size)
        assertEquals(1, task.destinationNodeDatabase.readMigrationData().transactions.size, "The transaction should have been copied from source to destination")
    }

    @Test
    fun `Data copies from source to destination database with all editors applied`() {
        val (snapshotDirectoryName,snapshotDirectory) = copyAndGetSnapshotDirectory("s1")
        val sourcePartyRepository = getPartyRepository(snapshotDirectoryName, "source")
        val destinationPartyRepository = getPartyRepository(snapshotDirectoryName, "destination")
        val identities = IdentityFactory.getIdentities(sourcePartyRepository, destinationPartyRepository)
        val serializer = getSerializer(snapshotDirectoryName)
        val sourceNodesDirectory = File(snapshotDirectory, "source")
        val destinationNodesDirectory = File(snapshotDirectory, "destination")

        val factory = NodesToNodesMigrationTaskFactory(sourceNodesDirectory, destinationNodesDirectory)
        val task = factory.getMigrationTasks().filter { it.sourceNodeDatabase.readMigrationData().transactions.size == 1 }.first()

        assertEquals(1, task.sourceNodeDatabase.readMigrationData().transactions.size)
        assertEquals(0, task.destinationNodeDatabase.readMigrationData().transactions.size)
        val cordappsRepository = getCordappsRepository("s1")
        DefaultMigration(task, getSerializer("s1"), cordappsRepository).run()
        val sourceMigrationData = task.sourceNodeDatabase.readMigrationData()
        val sourceNetworkParametersHash = task.sourceNodeDatabase.readNetworkParametersHash()
        val destinationMigrationData = task.destinationNodeDatabase.readMigrationData()
        val destinationNetworkParametersHash = task.destinationNodeDatabase.readNetworkParametersHash()
        assertEquals(1, sourceMigrationData.transactions.size)
        assertEquals(1, destinationMigrationData.transactions.size, "The transaction should have been copied from source to destination")
        verifyMigration(serializer, sourceMigrationData, destinationMigrationData, MigrationContext(identities, sourceNetworkParametersHash, destinationNetworkParametersHash))
    }

}