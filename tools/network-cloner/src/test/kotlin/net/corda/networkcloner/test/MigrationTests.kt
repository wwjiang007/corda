package net.corda.networkcloner.test

import net.corda.networkcloner.impl.NodesToNodesMigrationTaskFactory
import net.corda.networkcloner.runnable.Migration
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class MigrationTests : TestSupport() {

    @Test
    fun `Data copies from source to destination database`() {
        val snapshotDirectory = copyAndGetSnapshotDirectory("s2")
        val sourceNodesDirectory = File(snapshotDirectory, "source")
        val destinationNodesDirectory = File(snapshotDirectory, "destination")

        val factory = NodesToNodesMigrationTaskFactory(sourceNodesDirectory, destinationNodesDirectory)
        val task = factory.getMigrationTasks().single()

        assertEquals(1, task.sourceNodeDatabase.readMigrationData().transactions.size)
        assertEquals(0, task.destinationNodeDatabase.readMigrationData().transactions.size)
        Migration(task).run()
        assertEquals(1, task.sourceNodeDatabase.readMigrationData().transactions.size)
        assertEquals(1, task.destinationNodeDatabase.readMigrationData().transactions.size, "The transaction should have been copied from source to destination")
    }

}