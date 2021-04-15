package net.corda.networkcloner.entity

import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party

data class TransactionComponents(val inputs: List<StateRef>,
                                 val outputs: List<TransactionState<ContractState>>,
                                 val commands: List<Command<*>>,
                                 val attachments: List<SecureHash>,
                                 val notary: Party?,
                                 val timeWindow: TimeWindow?,
                                 val references: List<StateRef>,
                                 val networkParametersHash: SecureHash?) {


}
