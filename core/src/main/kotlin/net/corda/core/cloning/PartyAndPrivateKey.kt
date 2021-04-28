package net.corda.core.cloning

import net.corda.core.identity.Party
import java.security.KeyPair
import java.security.PrivateKey

data class PartyAndPrivateKey(val party: Party, val privateKey: PrivateKey) {

    val keyPair
    get() = KeyPair(party.owningKey, privateKey)

}
