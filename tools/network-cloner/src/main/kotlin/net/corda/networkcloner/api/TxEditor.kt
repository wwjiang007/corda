package net.corda.networkcloner.api

import net.corda.core.transactions.ComponentGroup
import net.corda.networkcloner.entity.Identity
import net.corda.networkcloner.entity.TransactionComponents

interface TxEditor {

    fun edit(transactionComponents : TransactionComponents, identityMappings: Map<Identity, Identity>) : TransactionComponents

}