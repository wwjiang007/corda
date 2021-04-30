package net.corda.networkcloner.api

import net.corda.core.crypto.SecureHash
import net.corda.networkcloner.entity.MigrationData

interface NodeDatabase {

    fun readMigrationData() : MigrationData
    fun writeMigrationData(migrationData: MigrationData)
    fun readNetworkParametersHash() : SecureHash

}