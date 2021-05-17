package net.corda.networkcloner.api

import net.corda.core.cloning.EntityMigration
import net.corda.core.cloning.TxEditor
import net.corda.nodeapi.internal.cordapp.CordappLoader

interface CordappsRepository {

    fun getTxEditors() : List<TxEditor>
    fun getEntityMigrations() : List<EntityMigration<*>>
    fun getCordappLoader() : CordappLoader

}