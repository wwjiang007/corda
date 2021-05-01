package net.corda.core.cloning

import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import java.lang.RuntimeException
import java.security.PublicKey

data class MigrationContext(val identities : List<Identity>, val sourceNetworkParametersHash : SecureHash, val destinationNetworkParametersHash : SecureHash) {

    fun findDestinationForSourceParty(party : AbstractParty) : AbstractParty {
        return identities.find { party == it.sourceParty }?.destinationPartyAndPrivateKey?.party ?: throw RuntimeException("Expected to find destination party for source party $party")
    }

    fun findDestinationForSourceOwningKey(sourceOwningKey : PublicKey) : PublicKey {
        return identities.find { sourceOwningKey == it.sourceParty.owningKey }?.destinationPartyAndPrivateKey?.party?.owningKey ?: throw RuntimeException("Expected to find destination owning key for source owning key ${sourceOwningKey}")
    }

}
