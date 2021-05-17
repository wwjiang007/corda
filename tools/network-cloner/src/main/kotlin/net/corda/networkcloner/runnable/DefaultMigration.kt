package net.corda.networkcloner.runnable

import net.corda.core.cloning.EntityMigration
import net.corda.core.cloning.TxEditor
import net.corda.networkcloner.api.CordappsRepository
import net.corda.networkcloner.api.Serializer
import net.corda.networkcloner.api.Signer
import net.corda.networkcloner.entity.MigrationTask
import net.corda.networkcloner.impl.txeditors.TxCommandsEditor
import net.corda.networkcloner.impl.txeditors.TxInputStatesEditor
import net.corda.networkcloner.impl.txeditors.TxNetworkParametersHashEditor
import net.corda.networkcloner.impl.txeditors.TxNotaryEditor
import net.corda.networkcloner.impl.txeditors.TxReferenceStatesEditor

open class DefaultMigration(migrationTask: MigrationTask, serializer: Serializer, signer: Signer, val cordappsRepository: CordappsRepository, dryRun : Boolean = false) : Migration(migrationTask, serializer, signer, dryRun) {

    override fun getTxEditors(): List<TxEditor> {
        val cordappTxEditors = cordappsRepository.getTxEditors()
        return cordappTxEditors + listOf(TxCommandsEditor(), TxNotaryEditor(), TxNetworkParametersHashEditor(), TxInputStatesEditor(), TxReferenceStatesEditor())
    }

    override fun getEntityMigrations(): List<EntityMigration<*>> {
        return cordappsRepository.getEntityMigrations()
    }
}