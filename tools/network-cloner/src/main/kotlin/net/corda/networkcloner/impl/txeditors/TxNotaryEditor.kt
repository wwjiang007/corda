package net.corda.networkcloner.impl.txeditors

import net.corda.core.cloning.Identity
import net.corda.core.cloning.TransactionComponents
import net.corda.core.cloning.TxEditor

class TxNotaryEditor : TxEditor {

    override fun edit(transactionComponents: TransactionComponents, identities: List<Identity>): TransactionComponents {
        return transactionComponents.notary?.let {
            transactionComponents.copy(notary = findDestinationForSourceParty(it, identities))
        } ?: transactionComponents
    }

}