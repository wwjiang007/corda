package net.corda.core.cloning

import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.createComponentGroups

data class TransactionComponents(val inputs: List<StateRef>, //not in simplest use case
                                 val outputs: List<TransactionState<ContractState>>, //Done
                                 val commands: List<Command<*>>,
                                 val attachments: List<SecureHash>,
                                 val notary: Party?,
                                 val timeWindow: TimeWindow?,
                                 val references: List<StateRef>,
                                 val networkParametersHash: SecureHash?) {

    fun toComponentGroups() = createComponentGroups(inputs, outputs, commands, attachments, notary, timeWindow, references, networkParametersHash)

}
