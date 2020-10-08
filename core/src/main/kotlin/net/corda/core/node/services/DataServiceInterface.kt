package net.corda.core.node.services

import net.corda.core.DoNotImplement
import net.corda.core.identity.Party

@DoNotImplement
interface DataServiceInterface {
    fun gossip(participants: List<Party>, isInitiatorNode: Boolean)
}