package net.corda.networkcloner.api

import net.corda.core.cloning.TxEditor
import net.corda.nodeapi.internal.cordapp.CordappLoader

interface CordappsRepository {

    fun getTxEditors() : List<TxEditor>
    fun getCordappLoader() : CordappLoader

}