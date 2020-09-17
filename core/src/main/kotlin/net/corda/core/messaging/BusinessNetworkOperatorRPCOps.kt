package net.corda.core.messaging

import net.corda.core.messaging.RPCOps

interface BusinessNetworkOperatorRPCOps : RPCOps {
    fun createBusinessNetwork() : String
}