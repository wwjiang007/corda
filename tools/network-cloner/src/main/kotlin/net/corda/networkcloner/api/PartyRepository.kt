package net.corda.networkcloner.api

import net.corda.core.cloning.PartyAndPrivateKey
import net.corda.core.identity.Party

interface PartyRepository {

    fun getParties() : List<Party>
    fun getPartiesWithPrivateKeys() : List<PartyAndPrivateKey>

}