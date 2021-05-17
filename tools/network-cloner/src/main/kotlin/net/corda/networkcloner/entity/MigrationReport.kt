package net.corda.networkcloner.entity

import net.corda.core.crypto.SecureHash

data class MigrationReport(val sourceToDestTxId : MutableMap<SecureHash, SecureHash>)
