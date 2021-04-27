package net.corda.networkcloner.api

import net.corda.networkcloner.entity.MigrationData

interface NodeDatabase {

    fun getMigrationData() : MigrationData

}