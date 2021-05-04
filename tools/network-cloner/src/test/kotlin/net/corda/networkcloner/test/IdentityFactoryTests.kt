package net.corda.networkcloner.test

import net.corda.networkcloner.impl.IdentitySpaceImpl
import net.corda.networkcloner.impl.NodesDirPartyRepository
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class IdentityFactoryTests : TestSupport() {

    @Test
    fun `Identities are correctly created`() {
        val sourceNodesDirectory = File(getSnapshotDirectory("s1"), "source")
        val sourcePartyRepository = NodesDirPartyRepository(sourceNodesDirectory)

        val destinationNodesDirectory = File(getSnapshotDirectory("s1"), "destination")
        val destinationPartyRepository = NodesDirPartyRepository(destinationNodesDirectory)

        val identities = IdentitySpaceImpl(sourcePartyRepository, destinationPartyRepository).getIdentities()
        assertEquals(3, identities.size)
        identities.forEach {
            assertEquals(it.sourceParty.name, it.destinationPartyAndPrivateKey.party.name)
            assertNotEquals(it.sourceParty.owningKey, it.destinationPartyAndPrivateKey.party.owningKey)
        }
    }

}