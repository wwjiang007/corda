package net.corda.networkcloner.util

import net.corda.networkcloner.api.PartyRepository
import net.corda.networkcloner.entity.Identity
import java.lang.RuntimeException

object IdentityFactory {

    fun getIdentities(sourcePartyRepository : PartyRepository, destinationPartyRepository: PartyRepository) : List<Identity> {
        val sourceParties = sourcePartyRepository.getParties()
        val destinationParties = destinationPartyRepository.getPartiesWithPrivateKeys()
        if (sourceParties.size != destinationParties.size) {
            throw RuntimeException("Expected to find same number of parties in source and destination")
        }

        return sourceParties.map { sourceParty ->
            val destinationParty = destinationParties.find { it.party.name == sourceParty.name } ?: throw RuntimeException("Couldn't find destination party for source party $sourceParty")
            Identity(sourceParty, destinationParty)
        }
    }

}