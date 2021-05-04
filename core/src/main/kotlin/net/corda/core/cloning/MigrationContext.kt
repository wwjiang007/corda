package net.corda.core.cloning

import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.lang.RuntimeException
import java.security.PublicKey

data class MigrationContext(val identitySpace : IdentitySpace, val sourceNetworkParametersHash : SecureHash, val destinationNetworkParametersHash : SecureHash) {



}
