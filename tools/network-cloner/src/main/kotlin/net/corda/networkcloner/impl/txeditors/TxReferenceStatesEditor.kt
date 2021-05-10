package net.corda.networkcloner.impl.txeditors

import net.corda.core.cloning.MigrationContext
import net.corda.core.cloning.TransactionComponents
import net.corda.core.cloning.TxEditor
import net.corda.core.contracts.StateRef
import net.corda.networkcloner.FailedAssumptionException

class TxReferenceStatesEditor : TxEditor {

    override fun edit(transactionComponents: TransactionComponents, migrationContext: MigrationContext): TransactionComponents {
        val newReferenceStates = transactionComponents.references.map {
            val newTxId = migrationContext.sourceTxIdToDestTxId[it.txhash] ?: throw FailedAssumptionException("Expected to find a destination transaction id for source transaction id ${it.txhash}")
            StateRef(newTxId, it.index)
        }
        return transactionComponents.copy(references = newReferenceStates)
    }

}