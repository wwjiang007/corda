package net.corda.networkcloner.impl.txeditors

import net.corda.core.cloning.MigrationContext
import net.corda.core.cloning.TransactionComponents
import net.corda.core.cloning.TxEditor

class TxNetworkParametersHashEditor : TxEditor {

    override fun edit(transactionComponents: TransactionComponents, migrationContext: MigrationContext): TransactionComponents {
        return transactionComponents.networkParametersHash?.let {
            transactionComponents.copy(networkParametersHash = migrationContext.destinationNetworkParametersHash)
        } ?: transactionComponents
    }

}