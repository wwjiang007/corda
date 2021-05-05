package net.corda.networkcloner.test

import net.corda.core.cloning.IdentitySpace
import net.corda.core.identity.Party
import net.corda.networkcloner.impl.IdentitySpaceImpl
import net.corda.networkcloner.impl.NodesDirPartyRepository
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File
import kotlin.math.exp
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class IdentityTests : TestSupport() {

    @Test
    fun `Identities are correctly created`() {
        val identities = getIdentitySpace("s1").getIdentities()
        assertEquals(3, identities.size)
        identities.forEach {
            assertEquals(it.sourceParty.name, it.destinationPartyAndPrivateKey.party.name)
            assertNotEquals(it.sourceParty.owningKey, it.destinationPartyAndPrivateKey.party.owningKey)
        }
    }

    @Test
    fun `xxx`() {
        val identitySpace = getIdentitySpace("s1")
        val sourceDb = getNodeDatabase("s1", "source", "client", identitySpace::getSourcePartyFromX500Name, identitySpace::getSourcePartyFromAnonymous)
        val sourceData = sourceDb.readMigrationData()

        assertEquals(2, sourceData.persistentParties.size)
        listOf(clientX500Name, operatorX500Name).forEach { expectedX500Name ->
            assertNotNull(sourceData.persistentParties.map { it.x500Name as Party }.find { it.name == expectedX500Name })
        }

        identitySpace.getIdentities().map { it.sourceParty.owningKey }.forEach { expectedOwningKey ->
            assertNotNull(sourceData.persistentParties.find { it.x500Name?.owningKey == expectedOwningKey })
        }
    }

    private fun getIdentitySpace(snapshot : String) : IdentitySpace {
        val sourceNodesDirectory = File(getSnapshotDirectory(snapshot), "source")
        val sourcePartyRepository = NodesDirPartyRepository(sourceNodesDirectory)

        val destinationNodesDirectory = File(getSnapshotDirectory(snapshot), "destination")
        val destinationPartyRepository = NodesDirPartyRepository(destinationNodesDirectory)

        return IdentitySpaceImpl(sourcePartyRepository, destinationPartyRepository)
    }

}