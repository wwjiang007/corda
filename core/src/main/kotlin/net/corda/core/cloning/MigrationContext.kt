package net.corda.core.cloning

import net.corda.core.crypto.SecureHash

data class MigrationContext(val identitySpace : IdentitySpace, val sourceNetworkParametersHash : SecureHash, val destinationNetworkParametersHash : SecureHash, val sourceTxIdToDestTxId : Map<SecureHash,SecureHash>)
