package net.corda.core.node.services

import net.corda.core.flows.SendGossipDataFlow
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class DataService (private val appServiceHub: AppServiceHub) : SingletonSerializeAsToken(), DataServiceInterface {
    override fun gossip(participants: List<Party>, isInitiatorNode: Boolean) {
        appServiceHub.startFlow(SendGossipDataFlow(participants, isInitiatorNode))
    }
}