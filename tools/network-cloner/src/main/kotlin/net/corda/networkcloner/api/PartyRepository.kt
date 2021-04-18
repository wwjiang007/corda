package net.corda.networkcloner.api

import net.corda.core.identity.Party
import net.corda.networkcloner.entity.PartyAndPrivateKey

interface PartyRepository {

    fun getParties() : List<Party>
    fun getPartiesWithPrivateKeys() : List<PartyAndPrivateKey>

}