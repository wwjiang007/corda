package net.corda.networkcloner.impl

import net.corda.core.cloning.Identity
import net.corda.core.cloning.IdentitySpace
import net.corda.core.cloning.PartyAndPrivateKey
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.networkcloner.api.PartyRepository
import java.lang.RuntimeException
import java.security.PublicKey

class IdentitySpaceImpl(sourcePartyRepository : PartyRepository, destinationPartyRepository: PartyRepository) : IdentitySpace {

    private val _identities : List<Identity>

    init {
        val sourceParties = sourcePartyRepository.getParties()
        val destinationParties = destinationPartyRepository.getPartiesWithPrivateKeys()
        if (sourceParties.size != destinationParties.size) {
            throw RuntimeException("Expected to find same number of parties in source and destination")
        }

        _identities = sourceParties.map { sourceParty ->
            val destinationParty = destinationParties.find { it.party.name == sourceParty.name } ?: throw RuntimeException("Couldn't find destination party for source party $sourceParty")
            Identity(sourceParty, destinationParty)
        }
    }

    override fun getIdentities(): List<Identity> {
        return _identities
    }

    override fun getSourcePartyFromX500Name(name: CordaX500Name): Party? {
        return _identities.find { it.sourceParty.name == name }?.sourceParty
    }

    override fun getSourcePartyFromAnonymous(party: AbstractParty): Party? {
        return _identities.find { it.sourceParty.owningKey == party.owningKey }?.sourceParty
    }

    override fun getDestinationPartyFromX500Name(name: CordaX500Name): Party? {
        return _identities.find { it.destinationPartyAndPrivateKey.party.name == name }?.destinationPartyAndPrivateKey?.party
    }

    override fun getDestinationPartyFromAnonymous(party: AbstractParty): Party? {
        return _identities.find { it.destinationPartyAndPrivateKey.party.owningKey == party.owningKey }?.destinationPartyAndPrivateKey?.party
    }

    override fun findDestinationForSourceParty(party : AbstractParty) : AbstractParty {
        return _identities.find { party == it.sourceParty }?.destinationPartyAndPrivateKey?.party ?: throw RuntimeException("Expected to find destination party for source party $party")
    }

    override fun findDestinationForSourceOwningKey(sourceOwningKey : PublicKey) : PublicKey {
        return _identities.find { sourceOwningKey == it.sourceParty.owningKey }?.destinationPartyAndPrivateKey?.party?.owningKey ?: throw RuntimeException("Expected to find destination owning key for source owning key ${sourceOwningKey}")
    }

    override fun getDestinationPartyAndPrivateKey(sourceOwningKey: PublicKey) : PartyAndPrivateKey {
        return _identities.find { sourceOwningKey == it.sourceParty.owningKey }?.destinationPartyAndPrivateKey ?: throw RuntimeException("Expected to find destination party and private key for owning key $sourceOwningKey")
    }
}