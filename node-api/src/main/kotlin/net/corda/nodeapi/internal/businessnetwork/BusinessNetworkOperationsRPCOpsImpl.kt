package net.corda.nodeapi.internal.businessnetwork

import net.corda.core.internal.PLATFORM_VERSION
import net.corda.core.node.NodeInfo
import net.corda.core.node.ServiceHub
import net.corda.core.utilities.contextLogger

class BusinessNetworkOperationsRPCOpsImpl(private val serviceHub: ServiceHub) : BusinessNetworkOperationsRPCOps {
    override val protocolVersion: Int = PLATFORM_VERSION

    companion object {
        private val logger = contextLogger()
    }

    override fun createBusinessNetwork() : String {
        val text = "Business network created!"
        logger.info(text)
        return text
    }

    override fun createGroup() : String {
        val text = "Business group created!"
        logger.info(text)
        return text
    }

    override fun getNodeInfo(): NodeInfo {
        return serviceHub.myInfo
    }
}