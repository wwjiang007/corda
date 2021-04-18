package net.corda.networkcloner.test

import net.corda.networkcloner.impl.NodesDirectoryPartyRepository
import net.corda.networkcloner.util.IdentityFactory
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class IdentityFactoryTests : TestSupport() {

    @Test
    fun `Identities are correctly created`() {
        val sourceNodesDirectory = File(getSnapshotDirectory("s1"), "source")
        val sourcePartyRepository = NodesDirectoryPartyRepository(sourceNodesDirectory)

        val destinationNodesDirectory = File(getSnapshotDirectory("s1"), "destination")
        val destinationPartyRepository = NodesDirectoryPartyRepository(destinationNodesDirectory)

        val identities = IdentityFactory.getIdentities(sourcePartyRepository, destinationPartyRepository)
        assertEquals(2, identities.size)
        identities.forEach {
            assertEquals(it.sourceParty.name, it.destinationPartyAndPrivateKey.party.name)
            assertNotEquals(it.sourceParty.owningKey, it.destinationPartyAndPrivateKey.party.owningKey)
        }
    }

}