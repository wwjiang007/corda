package net.corda.networkcloner.api

import net.corda.core.transactions.ComponentGroup
import java.security.PublicKey

interface TxEditor {

    fun replacePublicKey(componentGroups : List<ComponentGroup>, from : PublicKey, to : PublicKey) : List<ComponentGroup>

}