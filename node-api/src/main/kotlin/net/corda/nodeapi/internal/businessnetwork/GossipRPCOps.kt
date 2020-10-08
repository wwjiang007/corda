package net.corda.nodeapi.internal.businessnetwork

import net.corda.core.messaging.RPCOps

interface GossipRPCOps : RPCOps {
    fun gossiping(participants: String)
}