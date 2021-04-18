package net.corda.networkcloner.entity

import net.corda.networkcloner.api.TransactionsStore

data class MigrationTask(val identity: Identity,
                         val sourceTransactionStore: TransactionsStore,
                         val destinationTransactionStore: TransactionsStore,
                         val migrationContext: MigrationContext)
