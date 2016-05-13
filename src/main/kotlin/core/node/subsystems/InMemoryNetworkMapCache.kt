package core.node.subsystems

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import core.Contract
import core.Party
import core.crypto.SecureHash
import core.messaging.MessagingService
import core.messaging.StateMachineManager
import core.messaging.runOnNextMessage
import core.messaging.send
import core.node.NodeInfo
import core.node.services.*
import core.random63BitValue
import core.serialization.deserialize
import core.serialization.serialize
import core.utilities.AddOrRemove
import java.security.SignatureException
import java.util.*
import javax.annotation.concurrent.ThreadSafe

/**
 * Extremely simple in-memory cache of the network map.
 */
@ThreadSafe
open class InMemoryNetworkMapCache() : NetworkMapCache {
    override val networkMapNodes: List<NodeInfo>
        get() = get(NetworkMapService.Type)
    override val regulators: List<NodeInfo>
        get() = get(RegulatorService.Type)
    override val timestampingNodes: List<NodeInfo>
        get() = get(TimestamperService.Type)
    override val ratesOracleNodes: List<NodeInfo>
        get() = get(NodeInterestRates.Type)
    override val partyNodes: List<NodeInfo>
        get() = registeredNodes.map { it.value }

    private var registeredForPush = false
    protected var registeredNodes = Collections.synchronizedMap(HashMap<Party, NodeInfo>())

    override fun get() = registeredNodes.map { it.value }
    override fun get(serviceType: ServiceType) = registeredNodes.filterValues { it.advertisedServices.contains(serviceType) }.map { it.value }
    override fun getRecommended(type: ServiceType, contract: Contract, vararg party: Party): NodeInfo? = get(type).firstOrNull()

    override fun addMapService(smm: StateMachineManager, net: MessagingService, service: NodeInfo, subscribe: Boolean,
                               ifChangedSinceVer: Int?): ListenableFuture<Unit> {
        if (subscribe && !registeredForPush) {
            // Add handler to the network, for updates received from the remote network map service.
            net.addMessageHandler(NetworkMapService.PUSH_PROTOCOL_TOPIC + ".0", null) { message, r ->
                try {
                    val req = message.data.deserialize<NetworkMapService.Update>()
                    val hash = SecureHash.sha256(req.wireReg.serialize().bits)
                    val ackMessage = net.createMessage(NetworkMapService.PUSH_ACK_PROTOCOL_TOPIC + TOPIC_DEFAULT_POSTFIX,
                            NetworkMapService.UpdateAcknowledge(hash, net.myAddress).serialize().bits)
                    net.send(ackMessage, req.replyTo)
                    processUpdatePush(req)
                } catch(e: NodeMapError) {
                    NetworkMapCache.logger.warn("Failure during node map update due to bad update: ${e.javaClass.name}")
                } catch(e: Exception) {
                    NetworkMapCache.logger.error("Exception processing update from network map service", e)
                }
            }
            registeredForPush = true
        }

        // Fetch the network map and register for updates at the same time
        val sessionID = random63BitValue()
        val req = NetworkMapService.FetchMapRequest(subscribe, ifChangedSinceVer, net.myAddress, sessionID)

        // Add a message handler for the response, and prepare a future to put the data into.
        // Note that the message handler will run on the network thread (not this one).
        val future = SettableFuture.create<Unit>()
        net.runOnNextMessage("${NetworkMapService.FETCH_PROTOCOL_TOPIC}.$sessionID", MoreExecutors.directExecutor()) { message ->
            val resp = message.data.deserialize<NetworkMapService.FetchMapResponse>()
            // We may not receive any nodes back, if the map hasn't changed since the version specified
            resp.nodes?.forEach { processRegistration(it) }
            future.set(Unit)
        }
        net.send("${NetworkMapService.FETCH_PROTOCOL_TOPIC}.0", req, service.address)

        return future
    }

    override fun addNode(node: NodeInfo) {
        registeredNodes[node.identity] = node
    }

    override fun removeNode(node: NodeInfo) {
        registeredNodes.remove(node.identity)
    }

    /**
     * Unsubscribes from updates from the given map service.
     *
     * @param service the network map service to listen to updates from.
     */
    override fun deregisterForUpdates(smm: StateMachineManager, net: MessagingService, service: NodeInfo): ListenableFuture<Unit> {
        // Fetch the network map and register for updates at the same time
        val sessionID = random63BitValue()
        val req = NetworkMapService.SubscribeRequest(false, net.myAddress, sessionID)

        // Add a message handler for the response, and prepare a future to put the data into.
        // Note that the message handler will run on the network thread (not this one).
        val future = SettableFuture.create<Unit>()
        net.runOnNextMessage("${NetworkMapService.SUBSCRIPTION_PROTOCOL_TOPIC}.$sessionID", MoreExecutors.directExecutor()) { message ->
            val resp = message.data.deserialize<NetworkMapService.SubscribeResponse>()
            if (resp.confirmed) {
                future.set(Unit)
            } else {
                future.setException(NetworkCacheError.DeregistrationFailed())
            }
        }
        net.send("${NetworkMapService.SUBSCRIPTION_PROTOCOL_TOPIC}.0", req, service.address)

        return future
    }

    fun processUpdatePush(req: NetworkMapService.Update) {
        val reg: NodeRegistration
        try {
            reg = req.wireReg.verified()
        } catch(e: SignatureException) {
            throw NodeMapError.InvalidSignature()
        }
        processRegistration(reg)
    }

    private fun processRegistration(reg: NodeRegistration) {
        // TODO: Implement filtering by sequence number, so we only accept changes that are
        // more recent than the latest change we've processed.
        when (reg.type) {
            AddOrRemove.ADD -> addNode(reg.node)
            AddOrRemove.REMOVE -> removeNode(reg.node)
        }
    }
}