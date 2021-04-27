package net.corda.networkcloner.entity

import net.corda.networkcloner.api.NodeDatabase

data class MigrationTask(val identity: Identity,
                         val sourceTransactionStore: NodeDatabase,
                         val destinationTransactionStore: NodeDatabase,
                         val migrationContext: MigrationContext)
