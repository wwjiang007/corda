package net.corda.networkcloner.entity

import net.corda.core.cloning.AdditionalMigration
import net.corda.core.cloning.Identity
import net.corda.core.cloning.MigrationContext
import net.corda.networkcloner.api.NodeDatabase

data class MigrationTask(val identity: Identity,
                         val sourceNodeDatabase: NodeDatabase,
                         val destinationNodeDatabase: NodeDatabase,
                         val additionalMigrations: List<AdditionalMigration>,
                         val migrationContext: MigrationContext)
