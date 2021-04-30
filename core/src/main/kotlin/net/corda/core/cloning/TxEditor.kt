package net.corda.core.cloning

import net.corda.core.identity.Party
import java.lang.RuntimeException
import java.security.PublicKey

interface TxEditor {

    fun edit(transactionComponents : TransactionComponents, identities : List<Identity>) : TransactionComponents

    fun findDestinationForSourceParty(party : Party, identities: List<Identity>) : Party {
        return identities.find { party == it.sourceParty }?.destinationPartyAndPrivateKey?.party ?: throw RuntimeException("Expected to find destination party for source party ${party.name}")
    }

    fun findDestinationForSourceOwningKey(sourceOwningKey : PublicKey, identities: List<Identity>) : PublicKey {
        return identities.find { sourceOwningKey == it.sourceParty.owningKey }?.destinationPartyAndPrivateKey?.party?.owningKey ?: throw RuntimeException("Expected to find destination owning key for source owning key ${sourceOwningKey}")
    }

}