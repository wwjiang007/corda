package net.corda.networkcloner.util

import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.entity.TransactionComponents

fun SignedTransaction.toTransactionComponents() : TransactionComponents {
    val wTx = this.coreTransaction as WireTransaction
    return TransactionComponents(wTx.inputs, wTx.outputs, wTx.commands, wTx.attachments, wTx.notary, wTx.timeWindow, wTx.references, wTx.networkParametersHash)
}