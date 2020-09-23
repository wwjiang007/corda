package net.corda.nodeapi.internal.businessnetwork

import net.corda.core.messaging.RPCOps
import net.corda.core.node.NodeInfo

interface BusinessNetworkOperationsRPCOps : RPCOps {
    fun createBusinessNetwork() : String

    fun createGroup() : String

    fun getNodeInfo() : NodeInfo
}