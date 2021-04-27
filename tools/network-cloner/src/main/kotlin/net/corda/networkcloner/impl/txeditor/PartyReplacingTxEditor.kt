package net.corda.networkcloner.impl.txeditor

import net.corda.networkcloner.api.TxEditor
import net.corda.networkcloner.entity.Identity
import net.corda.networkcloner.entity.TransactionComponents

class PartyReplacingTxEditor : TxEditor {

    override fun edit(transactionComponents: TransactionComponents, identities: List<Identity>): TransactionComponents {
        return transactionComponents
    }
}