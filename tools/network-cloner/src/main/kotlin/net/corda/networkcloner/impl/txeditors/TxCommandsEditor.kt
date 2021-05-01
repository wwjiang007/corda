package net.corda.networkcloner.impl.txeditors

import net.corda.core.cloning.MigrationContext
import net.corda.core.cloning.TransactionComponents
import net.corda.core.cloning.TxEditor

class TxCommandsEditor : TxEditor {

    override fun edit(transactionComponents: TransactionComponents, migrationContext: MigrationContext): TransactionComponents {
        val newCommands = transactionComponents.commands.map {
            val newSigners = it.signers.map { signerPublicKey -> migrationContext.findDestinationForSourceOwningKey(signerPublicKey) }
            it.copy(signers = newSigners)
        }
        return transactionComponents.copy(commands = newCommands)
    }

}