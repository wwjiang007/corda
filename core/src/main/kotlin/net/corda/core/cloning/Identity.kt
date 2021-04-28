package net.corda.core.cloning

import net.corda.core.identity.Party

data class Identity(val sourceParty : Party, val destinationPartyAndPrivateKey: PartyAndPrivateKey)
