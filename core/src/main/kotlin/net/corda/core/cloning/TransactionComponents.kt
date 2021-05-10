package net.corda.core.cloning

import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.createComponentGroups

data class TransactionComponents(val txId: SecureHash,
                                 val inputs: List<StateRef>, //not in simplest use case
                                 val outputs: List<TransactionState<ContractState>>, //Done
                                 val commands: List<Command<*>>, //Done
                                 val attachments: List<SecureHash>, //shouldn't need changing
                                 val notary: Party?, //Done
                                 val timeWindow: TimeWindow?, //shouldn't need changing
                                 val references: List<StateRef>, //not in simplest use case
                                 val networkParametersHash: SecureHash?) {

    fun toComponentGroups() = createComponentGroups(inputs, outputs, commands, attachments, notary, timeWindow, references, networkParametersHash)

}
