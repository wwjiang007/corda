package net.corda.nodeapi.internal.businessnetwork

import net.corda.core.flows.bn.BNIdentity
import net.corda.core.flows.bn.Membership
import net.corda.core.messaging.RPCOps

interface BusinessNetworkOperationsRPCOps : RPCOps {

    fun createBusinessNetwork(): Membership

    fun onboardMembership(networkId: String, party: String, businessIdentity: BNIdentity? = null): Membership

    fun activateMembership(id: Long): Membership

    fun suspendMembership(id: Long): Membership

    fun revokeMembership(id: Long)

    fun getMembershipList(networkId: String): List<Membership>
}