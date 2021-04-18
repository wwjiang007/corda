package net.corda.networkcloner.test

import net.corda.networkcloner.impl.NodesDirPartyRepository
import net.corda.networkcloner.impl.NodesToNodesMigrationTaskFactory
import net.corda.networkcloner.util.IdentityFactory
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MigrationTaskFactoryTests : TestSupport() {

    @Test
    fun `Nodes to nodes migration task factory can produce migration tasks`() {
        val sourceNodesDirectory = File(getSnapshotDirectory("s1"), "source")
        val destinationNodesDirectory = File(getSnapshotDirectory("s1"), "destination")

        val factory = NodesToNodesMigrationTaskFactory(sourceNodesDirectory, destinationNodesDirectory)
        val tasks = factory.getMigrationTasks()

        assertEquals(2, tasks.size)
    }

}