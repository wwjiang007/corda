package net.corda.networkcloner.util

import net.corda.core.cloning.TransactionComponents
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction

fun SignedTransaction.toTransactionComponents() : TransactionComponents {
    val wTx = this.coreTransaction as WireTransaction
    return TransactionComponents(wTx.id, wTx.inputs, wTx.outputs, wTx.commands, wTx.attachments, wTx.notary, wTx.timeWindow, wTx.references, wTx.networkParametersHash)
}