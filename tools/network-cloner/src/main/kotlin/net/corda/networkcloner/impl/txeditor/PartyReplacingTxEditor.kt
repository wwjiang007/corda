package net.corda.networkcloner.impl.txeditor

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TransactionState
import net.corda.core.identity.AbstractParty
import net.corda.networkcloner.api.TxEditor
import net.corda.networkcloner.entity.Identity
import net.corda.networkcloner.entity.TransactionComponents
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

class PartyReplacingTxEditor : TxEditor {

    override fun edit(transactionComponents: TransactionComponents, identities: List<Identity>): TransactionComponents {
        editOutputs(transactionComponents.outputs, identities)
        return transactionComponents
    }

    private fun editOutputs(outputs : List<TransactionState<ContractState>>, identities: List<Identity>) {
        outputs.map { transactionState ->
            val data = transactionState.data
            data::class.memberProperties.filter {
                it.isAccessible = true
                val value = it.javaField?.get(data)
                value is AbstractParty || (value is Collection<*> && value.all { it is AbstractParty })
            }.forEach {
                replaceValue(data, it, identities)
                println("The field is ${it.name}")
            }
        }
    }

    private fun replaceValue(obj : Any, field : KProperty1<out ContractState, Any?>, identities: List<Identity>) {
        val currentValue =  field.javaField?.get(obj)

    }
}