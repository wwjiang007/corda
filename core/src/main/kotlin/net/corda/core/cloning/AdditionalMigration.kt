package net.corda.core.cloning

interface AdditionalMigration {

    fun getManagedClasses() : List<Class<*>>
    fun migrate(source : NodeDb, destination: NodeDb, migrationContext: MigrationContext)

}