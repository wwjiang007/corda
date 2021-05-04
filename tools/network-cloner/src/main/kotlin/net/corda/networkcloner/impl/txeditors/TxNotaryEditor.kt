package net.corda.networkcloner.impl.txeditors

import net.corda.core.cloning.Identity
import net.corda.core.cloning.MigrationContext
import net.corda.core.cloning.TransactionComponents
import net.corda.core.cloning.TxEditor
import net.corda.core.identity.Party

class TxNotaryEditor : TxEditor {

    override fun edit(transactionComponents: TransactionComponents, migrationContext: MigrationContext): TransactionComponents {
        return transactionComponents.notary?.let {
            transactionComponents.copy(notary = migrationContext.identitySpace.findDestinationForSourceParty(it) as Party)
        } ?: transactionComponents
    }

}