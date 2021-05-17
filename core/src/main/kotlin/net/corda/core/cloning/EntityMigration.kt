package net.corda.core.cloning

interface EntityMigration<T : Any> {

    fun getEntityClass() : Class<T>
    fun migrate(entities : List<T>, migrationContext: MigrationContext)

}