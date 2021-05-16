package net.corda.networkcloner.api

import net.corda.core.cloning.AdditionalMigration
import net.corda.core.cloning.TxEditor
import net.corda.nodeapi.internal.cordapp.CordappLoader
import java.net.URL

interface CordappsRepository {

    fun getTxEditors() : List<TxEditor>
    fun getAdditionalMigrations() : List<AdditionalMigration>
    fun getCordappLoader() : CordappLoader
    fun getCordappsURLs() : List<URL>

}