package net.corda.networkcloner.api

import net.corda.core.crypto.SecureHash
import net.corda.networkcloner.entity.CoreCordaData

interface NodeDatabase {

    fun readMigrationData() : CoreCordaData
    fun writeMigrationData(coreCordaData: CoreCordaData)
    fun readNetworkParametersHash() : SecureHash

}