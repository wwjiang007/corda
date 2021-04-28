package net.corda.networkcloner.api

import net.corda.core.cloning.TxEditor

interface TxEditorFactory {

    fun getTxEditors() : List<TxEditor>

}