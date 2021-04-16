package net.corda.networkcloner.test

import org.junit.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class IdentityRepositoryTests : TestSupport() {

    @Test
    fun `JKS files can be read and parsed into identities`() {
        val identityMapper = getIdentityMapper("s1")

        val operatorIdentity = identityMapper.getIdentityBySourceX500Name(operatorX500Name)
        val clientIdentity = identityMapper.getIdentityBySourceX500Name(clientX500Name)
        assertNotNull(operatorIdentity)
        assertNotNull(clientIdentity)
        assertNotEquals(operatorIdentity!!.sourceParty.owningKey, operatorIdentity!!.destinationPartyAndPrivateKey.party.owningKey)
        assertNotEquals(clientIdentity!!.sourceParty.owningKey, clientIdentity!!.destinationPartyAndPrivateKey.party.owningKey)
    }

}