package net.corda.networkcloner.test

import net.corda.networkcloner.impl.NodesDirectoryPartyRepository
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class PartyRepositoryTests : TestSupport() {

    @Test
    fun `Parties can be loaded via NodesDirectoryPartyRepository`() {
        val nodesDirectory = File(getSnapshotDirectory("s1"), "source")
        val partyRepository = NodesDirectoryPartyRepository(nodesDirectory)
        val parties = partyRepository.getParties()
        assertEquals(2, parties.size)
        val clientParty = parties.find { it.name == clientX500Name }
        val operatorParty = parties.find { it.name == operatorX500Name }
        assertNotNull(clientParty)
        assertNotNull(operatorParty)
        assertNotEquals(clientParty!!.owningKey, operatorParty!!.owningKey)
    }

    @Test
    fun `Parties with private keys can be loaded via NodesDirectoryPartyRepository`() {
        val nodesDirectory = File(getSnapshotDirectory("s1"), "destination")
        val partyRepository = NodesDirectoryPartyRepository(nodesDirectory)
        val partiesWithPrivateKeys = partyRepository.getPartiesWithPrivateKeys()
        assertEquals(2, partiesWithPrivateKeys.size)
        val clientParty = partiesWithPrivateKeys.find { it.party.name == clientX500Name }
        val operatorParty = partiesWithPrivateKeys.find { it.party.name == operatorX500Name }
        assertNotNull(clientParty)
        assertNotNull(operatorParty)
        assertNotEquals(clientParty!!.privateKey, operatorParty!!.privateKey)
        assertNotEquals(clientParty!!.party.owningKey, operatorParty!!.party.owningKey)
    }

}