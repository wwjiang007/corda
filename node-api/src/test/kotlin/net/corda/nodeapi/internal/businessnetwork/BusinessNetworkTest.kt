package net.corda.nodeapi.internal.businessnetwork

import net.corda.client.rpc.ext.MultiRPCClient
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.Permissions
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeParameters
import net.corda.testing.driver.driver
import net.corda.testing.driver.internal.incrementalPortAllocation
import net.corda.testing.node.User
import org.junit.Test
import kotlin.test.assertEquals

class BusinessNetworkTest {
    @Test(timeout = 300_000)
    fun `try custom bn rpc`() {
        val user = User("u", "p", setOf(Permissions.all()))

        val rpcAddress = incrementalPortAllocation().nextHostAndPort()

        val client = MultiRPCClient(rpcAddress, BusinessNetworkOperationsRPCOps::class.java,
                user.username, user.password)

        client.use {
             driver(DriverParameters(notarySpecs = emptyList(), startNodesInProcess = false)) {
                startNode(providedName = ALICE_NAME,
                        defaultParameters = NodeParameters(rpcAddress = rpcAddress, rpcUsers = listOf(user))).getOrThrow()

                val conn = client.start().get()

                conn.use {
                    val text = it.proxy.createBusinessNetwork()
                    assertEquals("Business network created!", text)
                    val nodeInfo = it.proxy.getNodeInfo()
                    assertEquals(ALICE_NAME, nodeInfo.legalIdentities.first().name)
                }
            }
        }
    }
}