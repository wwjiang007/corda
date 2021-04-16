package net.corda.networkcloner.entity

import net.corda.core.identity.Party

data class Identity(val sourceParty : Party, val destinationPartyAndPrivateKey: PartyAndPrivateKey)
