package net.corda.core.node.services

import net.corda.core.flows.SendGossipDataFlow
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.AppServiceHub.Companion.SERVICE_PRIORITY_NORMAL
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.contextLogger
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@CordaService
class DataService(private val appServiceHub: AppServiceHub) : SingletonSerializeAsToken(), DataServiceInterface {

    private val dataObserver = DataObserver()

    companion object {
        val log = contextLogger()
        val executor: Executor = Executors.newFixedThreadPool(8)!!
    }

    init {
        appServiceHub.register(SERVICE_PRIORITY_NORMAL, DataObserver())
    }

    override fun gossip(participants: List<Party>, isInitiatorNode: Boolean) {
        executor.execute {
            appServiceHub.startFlow(SendGossipDataFlow(participants, isInitiatorNode))
        }
    }
}

class DataObserver : ServiceLifecycleObserver {
    companion object {
        val log = contextLogger()
    }

    override fun onServiceLifecycleEvent(event: ServiceLifecycleEvent) {
        when (event) {
            ServiceLifecycleEvent.STATE_MACHINE_STARTED -> log.info("STATE_MACHINE_STARTED received")
        }
    }
}