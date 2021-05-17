package net.corda.networkcloner.test

import net.corda.core.cloning.MigrationContext
import net.corda.core.cloning.EntityMigration
import net.corda.core.cloning.TxEditor
import net.corda.core.crypto.SecureHash
import net.corda.core.schemas.PersistentState
import net.corda.networkcloner.FailedAssumptionException
import net.corda.networkcloner.api.NodeDatabase
import net.corda.networkcloner.impl.IdentitySpaceImpl
import net.corda.networkcloner.impl.NodesToNodesMigrationTaskFactory
import net.corda.networkcloner.runnable.DefaultMigration
import net.corda.networkcloner.runnable.Migration
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MigrationTests : TestSupport() {

    @Test
    fun `Data copies from source to destination database`() {
        val snapshotDirectory = copyAndGetSnapshotDirectory("s1").second
        val sourceNodesDirectory = File(snapshotDirectory, "source")
        val destinationNodesDirectory = File(snapshotDirectory, "destination")

        val factory = NodesToNodesMigrationTaskFactory(sourceNodesDirectory, destinationNodesDirectory, getCordappsRepository())
        val task = factory.getMigrationTasks().filter { it.sourceNodeDatabase.readCoreCordaData().transactions.size == 1 }.first()

        assertEquals(1, task.sourceNodeDatabase.readCoreCordaData().transactions.size)
        assertEquals(0, task.destinationNodeDatabase.readCoreCordaData().transactions.size)
        val noOpMigration = object : Migration(task, getSerializer(), getSigner(), false) {
            override fun getTxEditors(): List<TxEditor> = emptyList()
            override fun getEntityMigrations(): List<EntityMigration<*>> = emptyList()
        }
        noOpMigration.call()
        val sourceMigrationData = task.sourceNodeDatabase.readCoreCordaData()
        val destinationMigrationData = task.destinationNodeDatabase.readCoreCordaData()
        assertEquals(1, sourceMigrationData.transactions.size)
        assertEquals(1, destinationMigrationData.transactions.size, "The transaction should have been copied from source to destination")
        assertEquals(2, sourceMigrationData.persistentParties.size)
        assertEquals(2, destinationMigrationData.persistentParties.size, "The persistent parties should have been copied from source to destination")
        assertEquals(1, sourceMigrationData.vaultLinearStates.size)
        assertEquals(1, destinationMigrationData.vaultLinearStates.size, "The vault linear states should have been copied from source to destination")
        assertEquals(1, sourceMigrationData.vaultStates.size)
        assertEquals(1, destinationMigrationData.vaultStates.size, "The vault states should have been copied from source to destination")
        assertEquals(1, sourceMigrationData.dbAttachments.size)
        assertEquals(1, destinationMigrationData.dbAttachments.size, "The identical attachment shouldn't have resulted in any change")
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

        val factory = NodesToNodesMigrationTaskFactory(sourceNodesDirectory, destinationNodesDirectory, getCordappsRepository())
        val task = factory.getMigrationTasks().filter { it.identity.sourceParty.name.toString().contains("client", true) }.first()

        assertEquals(1, task.sourceNodeDatabase.readCoreCordaData().transactions.size)
        assertEquals(0, task.destinationNodeDatabase.readCoreCordaData().transactions.size)
        val cordappsRepository = getCordappsRepository()
        object : DefaultMigration(task, getSerializer(), getSigner(), cordappsRepository) {
            override fun getEntityMigrations(): List<EntityMigration<*>> {
                return emptyList()
            }
        }.call()
        val sourceMigrationData = task.sourceNodeDatabase.readCoreCordaData()
        val sourceNetworkParametersHash = task.sourceNodeDatabase.readNetworkParametersHash()
        val destinationMigrationData = task.destinationNodeDatabase.readCoreCordaData()
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

        val factory = NodesToNodesMigrationTaskFactory(sourceNodesDirectory, destinationNodesDirectory, getCordappsRepository())
        val migrationTasks = factory.getMigrationTasks()
        assertEquals(3, migrationTasks.size)
        val task = migrationTasks.filter { it.identity.sourceParty.name.toString().contains("client", true) }.single()

        assertEquals(3, task.sourceNodeDatabase.readCoreCordaData().transactions.size)
        assertEquals(0, task.destinationNodeDatabase.readCoreCordaData().transactions.size)
        val cordappsRepository = getCordappsRepository()
        object : DefaultMigration(task, getSerializer(), getSigner(), cordappsRepository) {
            override fun getEntityMigrations(): List<EntityMigration<*>> {
                return emptyList()
            }
        }.call()
        val sourceMigrationData = task.sourceNodeDatabase.readCoreCordaData()
        val sourceNetworkParametersHash = task.sourceNodeDatabase.readNetworkParametersHash()
        val destinationMigrationData = task.destinationNodeDatabase.readCoreCordaData()
        val destinationNetworkParametersHash = task.destinationNodeDatabase.readNetworkParametersHash()
        assertEquals(3, sourceMigrationData.transactions.size)
        assertEquals(3, destinationMigrationData.transactions.size, "The transaction should have been copied from source to destination")
        verifyMigration(serializer, sourceMigrationData, destinationMigrationData, MigrationContext(identitySpace, sourceNetworkParametersHash, destinationNetworkParametersHash, emptyMap()))
    }

    @Test
    fun `Attachments copy from source to destination database`() {
        val snapshotDirectory = copyAndGetSnapshotDirectory("s4-attachments").second
        val sourceNodesDirectory = File(snapshotDirectory, "source")
        val destinationNodesDirectory = File(snapshotDirectory, "destination")

        val factory = NodesToNodesMigrationTaskFactory(sourceNodesDirectory, destinationNodesDirectory, getCordappsRepository())
        val task = factory.getMigrationTasks().filter { it.identity.sourceParty.name.toString().contains("client", true) }.single()

        assertEquals(3, task.sourceNodeDatabase.readCoreCordaData().dbAttachments.size)
        assertEquals(1, task.destinationNodeDatabase.readCoreCordaData().dbAttachments.size)
        val noOpMigration = object : Migration(task, getSerializer(), getSigner(), false) {
            override fun getTxEditors(): List<TxEditor> = emptyList()
            override fun getEntityMigrations(): List<EntityMigration<*>> = emptyList()
        }
        noOpMigration.call()
        val sourceMigrationData = task.sourceNodeDatabase.readCoreCordaData()
        val destinationMigrationData = task.destinationNodeDatabase.readCoreCordaData()
        assertEquals(3, sourceMigrationData.dbAttachments.size)
        assertEquals(3, destinationMigrationData.dbAttachments.size, "The attachments should have been copied from source to destination")
        sourceMigrationData.dbAttachments.forEach { sourceAttachment ->
            val destinationAttachment = destinationMigrationData.dbAttachments.find { it.attId == sourceAttachment.attId } ?: throw FailedAssumptionException("Expected to find attachment in destination database for source attachment id ${sourceAttachment.attId}")
            assertEquals(3, destinationAttachment.contractClassNames?.size)
            assertEquals(1, destinationAttachment.signers?.size)

        }
    }

    @Test
    fun `Entity migrations copy from source to destination database`() {
        val snapshotDirectory = copyAndGetSnapshotDirectory("s5-mapped-schema-table").second
        val sourceNodesDirectory = File(snapshotDirectory, "source")
        val destinationNodesDirectory = File(snapshotDirectory, "destination")

        val cordappsRepository = getCordappsRepository()
        val factory = NodesToNodesMigrationTaskFactory(sourceNodesDirectory, destinationNodesDirectory, cordappsRepository)
        val task = factory.getMigrationTasks().filter { it.identity.sourceParty.name.toString().contains("client", true) }.single()

        val migration = DefaultMigration(task, getSerializer(), getSigner(), getCordappsRepository(), false)
        @Suppress("UNCHECKED_CAST")
        val persistentReceiptStateClass = cordappsRepository.getCordappLoader().appClassLoader.loadClass("com.r3.corda.lib.contracts.contractsdk.testapp.contracts.ReceiptSchemaV1\$PersistentReceiptState") as Class<PersistentState>

        @Suppress("UNCHECKED_CAST")
        val sourceEntitiesBefore = task.sourceNodeDatabase.getEntities(persistentReceiptStateClass) as List<PersistentState>
        @Suppress("UNCHECKED_CAST")
        val destEntitiesBefore = task.destinationNodeDatabase.getEntities(persistentReceiptStateClass) as List<PersistentState>
        assertEquals(1, sourceEntitiesBefore.size)
        assertEquals(0, destEntitiesBefore.size)
        val migrationReport = migration.call()
        @Suppress("UNCHECKED_CAST")
        val sourceEntitiesAfter = task.sourceNodeDatabase.getEntities(persistentReceiptStateClass) as List<PersistentState>
        @Suppress("UNCHECKED_CAST")
        val destEntitiesAfter = task.destinationNodeDatabase.getEntities(persistentReceiptStateClass) as List<PersistentState>
        assertEquals(1, sourceEntitiesAfter.size)
        assertEquals(1, destEntitiesAfter.size)

        sourceEntitiesBefore.forEach { sourceEntity ->
            val sourceTxId = sourceEntity.stateRef?.txId ?: throw FailedAssumptionException("Expected to find stateRef on the source entity")
            val expectedDestTxId = migrationReport.sourceToDestTxId[SecureHash.parse(sourceTxId)] ?: throw FailedAssumptionException("Expected to find dest tx id for source tx id $sourceTxId")
            assertNotNull(destEntitiesAfter.find { it.stateRef?.txId == expectedDestTxId.toString() }, "Expected to find destination entity with txId $expectedDestTxId")
        }

    }

    fun NodeDatabase.getEntities(type : Class<out Any>) : List<Any> {
        return readMigrationData(listOf(type)).entities[type] ?: throw FailedAssumptionException("Assumed to find entry for class $type")
    }

}