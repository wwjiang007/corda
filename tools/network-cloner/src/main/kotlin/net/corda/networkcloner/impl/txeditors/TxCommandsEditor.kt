package net.corda.networkcloner.impl.txeditors

import net.corda.core.cloning.Identity
import net.corda.core.cloning.TransactionComponents
import net.corda.core.cloning.TxEditor

class TxCommandsEditor : TxEditor {

    override fun edit(transactionComponents: TransactionComponents, identities: List<Identity>): TransactionComponents {
        val newCommands = transactionComponents.commands.map {
            val newSigners = it.signers.map { signerPublicKey -> findDestinationForSourceOwningKey(signerPublicKey, identities) }
            it.copy(signers = newSigners)
        }
        return transactionComponents.copy(commands = newCommands)
    }

}