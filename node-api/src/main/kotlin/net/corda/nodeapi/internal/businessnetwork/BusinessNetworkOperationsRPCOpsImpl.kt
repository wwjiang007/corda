package net.corda.nodeapi.internal.businessnetwork

import net.corda.core.flows.bn.ActivateMembership
import net.corda.core.flows.bn.BNIdentity
import net.corda.core.flows.bn.CreateBusinessNetwork
import net.corda.core.flows.bn.GetMembershipList
import net.corda.core.flows.bn.Membership
import net.corda.core.flows.bn.OnboardMembership
import net.corda.core.flows.bn.RevokeMembership
import net.corda.core.flows.bn.SuspendMembership
import net.corda.core.internal.PLATFORM_VERSION
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.utilities.getOrThrow

class BusinessNetworkOperationsRPCOpsImpl(private val cordaRPCOps: CordaRPCOps) : BusinessNetworkOperationsRPCOps {

    override val protocolVersion: Int = PLATFORM_VERSION

    override fun createBusinessNetwork(): Membership =
            cordaRPCOps.startTrackedFlow(::CreateBusinessNetwork).returnValue.getOrThrow()

    override fun onboardMembership(networkId: String, party: String, businessIdentity: BNIdentity?): Membership =
            cordaRPCOps.startTrackedFlow(::OnboardMembership, networkId, cordaRPCOps.partiesFromName(party, false).single(), businessIdentity).returnValue.getOrThrow()

    override fun activateMembership(id: Long): Membership = cordaRPCOps.startTrackedFlow(::ActivateMembership, id).returnValue.getOrThrow()

    override fun suspendMembership(id: Long): Membership = cordaRPCOps.startTrackedFlow(::SuspendMembership, id).returnValue.getOrThrow()

    override fun revokeMembership(id: Long) = cordaRPCOps.startTrackedFlow(::RevokeMembership, id).returnValue.getOrThrow()

    override fun getMembershipList(networkId: String): List<Membership> =
            cordaRPCOps.startTrackedFlow(::GetMembershipList, networkId).returnValue.getOrThrow()
}