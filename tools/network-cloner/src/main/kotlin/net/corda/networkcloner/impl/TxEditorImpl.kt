package net.corda.networkcloner.impl

import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.TransactionState
import net.corda.core.internal.deserialiseComponentGroup
import net.corda.core.transactions.ComponentGroup
import net.corda.networkcloner.api.TxEditor
import java.security.PublicKey

class TxEditorImpl : TxEditor {

    override fun replacePublicKey(componentGroups: List<ComponentGroup>, from: PublicKey, to: PublicKey) : List<ComponentGroup> {
        val outputs = deserialiseComponentGroup(componentGroups, TransactionState::class, ComponentGroupEnum.OUTPUTS_GROUP)
        return emptyList()
    }
}