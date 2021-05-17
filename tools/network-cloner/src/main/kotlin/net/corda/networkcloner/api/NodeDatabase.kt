package net.corda.networkcloner.api

import net.corda.core.crypto.SecureHash
import net.corda.networkcloner.entity.CoreCordaData
import net.corda.networkcloner.entity.MigrationData

interface NodeDatabase {

    fun readMigrationData(entityClasses : List<Class<out Any>>) : MigrationData
    fun readCoreCordaData() : CoreCordaData
    fun writeMigrationData(migrationData: MigrationData)
    fun writeCoreCordaData(coreCordaData: CoreCordaData)
    fun readNetworkParametersHash() : SecureHash

}