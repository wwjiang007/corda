package net.corda.core.cloning

import javax.persistence.EntityManager

interface AdditionalMigration {

    fun getManagedClasses() : List<Class<*>>
    fun migrate(source : EntityManager, destination: EntityManager, migrationContext: MigrationContext)

}