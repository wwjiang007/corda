package net.corda.testing.node

import net.corda.core.flows.SendGossipDataFlow
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.DataServiceInterface
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class MockDataService : SingletonSerializeAsToken(), DataServiceInterface {
    override fun gossip(participants: List<Party>, isInitiatorNode: Boolean) {}
}