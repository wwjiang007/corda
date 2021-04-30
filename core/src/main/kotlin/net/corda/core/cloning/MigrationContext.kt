package net.corda.core.cloning

import net.corda.core.crypto.SecureHash

data class MigrationContext(val identities : List<Identity>, val sourceNetworkParametersHash : SecureHash, val destinationNetworkParametersHash : SecureHash)
