package net.corda.core.cloning

interface TxEditor {

    fun edit(transactionComponents : TransactionComponents, migrationContext: MigrationContext) : TransactionComponents

}