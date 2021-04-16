package net.corda.networkcloner.test.txeditors

import net.corda.core.identity.AbstractParty
import net.corda.networkcloner.api.TxEditor
import net.corda.networkcloner.entity.Identity
import net.corda.networkcloner.entity.TransactionComponents
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

class TestAppTxEditor : TxEditor {

    override fun edit(transactionComponents : TransactionComponents, identityMappings: Map<Identity, Identity>): TransactionComponents {
        transactionComponents.outputs.map { transactionState ->
            val data = transactionState.data
            data::class.memberProperties.filter {
                it.isAccessible = true
                it.javaField?.get(data) is AbstractParty
            }.forEach {

            }
        }

        return transactionComponents
    }

}