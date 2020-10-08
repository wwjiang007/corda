package net.corda.nodeapi.internal.businessnetwork

import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.PLATFORM_VERSION
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.DataService

class GossipRPCOpsImpl(serviceHub: ServiceHub, val rpcOps: CordaRPCOps) : GossipRPCOps {

    private val dataService = serviceHub.cordaService(DataService::class.java)
    override val protocolVersion: Int = PLATFORM_VERSION

    override fun gossiping(participants: String) {
        val participantList = participants.split(";").mapNotNull {
            rpcOps.wellKnownPartyFromX500Name(CordaX500Name.parse(it))
        }

        dataService.gossip(participantList, true)
    }
}