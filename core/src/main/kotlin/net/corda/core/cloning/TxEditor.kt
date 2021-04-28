package net.corda.core.cloning

interface TxEditor {

    fun edit(transactionComponents : TransactionComponents, identities : List<Identity>) : TransactionComponents

}