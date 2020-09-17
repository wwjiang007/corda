package net.corda.node.internal.bnsample

import net.corda.core.internal.PLATFORM_VERSION
import net.corda.core.messaging.BusinessNetworkOperatorRPCOps
import net.corda.core.utilities.contextLogger

class BusinessNetworkOperatorRPCOpsImpl : BusinessNetworkOperatorRPCOps {
    override val protocolVersion: Int = PLATFORM_VERSION

    companion object {
        private val logger = contextLogger()
    }

    override fun createBusinessNetwork() : String {
        val text = "Business network created!"
        logger.info(text)
        return text
    }
}