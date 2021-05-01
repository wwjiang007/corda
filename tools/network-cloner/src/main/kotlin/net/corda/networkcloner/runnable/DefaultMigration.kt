package net.corda.networkcloner.runnable

import net.corda.core.cloning.TxEditor
import net.corda.networkcloner.api.CordappsRepository
import net.corda.networkcloner.api.Serializer
import net.corda.networkcloner.entity.MigrationTask
import net.corda.networkcloner.impl.txeditors.TxCommandsEditor
import net.corda.networkcloner.impl.txeditors.TxNetworkParametersHashEditor
import net.corda.networkcloner.impl.txeditors.TxNotaryEditor

class DefaultMigration(migrationTask: MigrationTask, serializer: Serializer, val cordappsRepository: CordappsRepository) : Migration(migrationTask, serializer) {

    override fun getTxEditors(): List<TxEditor> {
        val cordappTxEditors = cordappsRepository.getTxEditors()
        return cordappTxEditors + listOf(TxCommandsEditor(), TxNotaryEditor(), TxNetworkParametersHashEditor())
    }
}