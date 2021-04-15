package net.corda.networkcloner.test.txeditors

import net.corda.networkcloner.api.TxEditor
import net.corda.networkcloner.entity.Identity
import net.corda.networkcloner.entity.TransactionComponents

class TestAppTxEditor : TxEditor {

    override fun edit(transactionComponents : TransactionComponents, identityMappings: Map<Identity, Identity>): TransactionComponents {
        return transactionComponents
    }

}