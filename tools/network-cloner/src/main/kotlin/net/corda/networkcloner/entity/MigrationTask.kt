package net.corda.networkcloner.entity

import org.h2.mvstore.tx.TransactionStore

data class MigrationTask(val identity: Identity,
                         val sourceTransactionStore: TransactionStore,
                         val destinationTransactionStore: TransactionStore,
                         val migrationContext: MigrationContext)
