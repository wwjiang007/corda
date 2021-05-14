package net.corda.networkcloner.api

import net.corda.core.cloning.NodeDb
import net.corda.core.crypto.SecureHash
import net.corda.networkcloner.entity.CoreCordaData

interface NodeDatabase {

    fun readCoreCordaData() : CoreCordaData
    fun writeCoreCordaData(coreCordaData: CoreCordaData)
    fun readNetworkParametersHash() : SecureHash
    fun getNarrowDb() : NodeDb

}