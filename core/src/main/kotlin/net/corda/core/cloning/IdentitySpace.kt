package net.corda.core.cloning

import net.corda.core.cloning.Identity
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.security.PublicKey

interface IdentitySpace {

    fun getIdentities() : List<Identity>

    fun getSourcePartyFromX500Name(name: CordaX500Name): Party?

    fun getSourcePartyFromAnonymous(party: AbstractParty): Party?

    fun getDestinationPartyFromX500Name(name: CordaX500Name): Party?

    fun getDestinationPartyFromAnonymous(party: AbstractParty): Party?

    fun findDestinationForSourceParty(party : AbstractParty) : AbstractParty

    fun findDestinationForSourceOwningKey(sourceOwningKey : PublicKey) : PublicKey

    fun getDestinationPartyAndPrivateKey(sourceOwningKey: PublicKey) : PartyAndPrivateKey

}