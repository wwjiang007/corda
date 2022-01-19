package net.corda.coretests.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TimeWindow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.NotaryException
import net.corda.core.flows.NotaryFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty
import net.corda.core.messaging.startFlow
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.days
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.seconds
import net.corda.node.services.Permissions
import net.corda.testing.contracts.DummyContract
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.singleIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.NotarySpec
import net.corda.testing.node.User
import net.corda.testing.node.internal.DUMMY_CONTRACTS_CORDAPP
import net.corda.testing.node.internal.enclosedCordapp
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class NotaryFlowClientTest {

    private companion object {
        val user = User("mark", "dadada", setOf(Permissions.all()))
        val cordapps = listOf(enclosedCordapp(), DUMMY_CONTRACTS_CORDAPP)
    }

    @Test(timeout = 300_000)
    fun `non-validating notary conflict error causes the flow to fail`() {
        driver(
            DriverParameters(
                startNodesInProcess = true,
                notarySpecs = listOf(NotarySpec(DUMMY_NOTARY_NAME, validating = false)),
                cordappsForAllNodes = cordapps
            )
        ) {
            val alice = startNode(providedName = ALICE_NAME, rpcUsers = listOf(user)).getOrThrow()
            assertThrows<NotaryException> {
                alice.rpc.startFlow(::ConflictFlow).returnValue.getOrThrow(20.seconds)
            }
        }
    }

    @Test(timeout = 300_000)
    fun `non-validating notary time window invalid error causes the flow to fail`() {
        driver(
            DriverParameters(
                startNodesInProcess = true,
                notarySpecs = listOf(NotarySpec(DUMMY_NOTARY_NAME, validating = false)),
                cordappsForAllNodes = cordapps
            )
        ) {
            val alice = startNode(providedName = ALICE_NAME, rpcUsers = listOf(user)).getOrThrow()
            assertThrows<NotaryException> {
                alice.rpc.startFlow(::InvalidTimeWindowFlow).returnValue.getOrThrow(20.seconds)
            }
        }
    }

    @Test(timeout = 300_000)
    fun `validating notary conflict error causes the flow to fail`() {
        driver(
            DriverParameters(
                startNodesInProcess = true,
                notarySpecs = listOf(NotarySpec(DUMMY_NOTARY_NAME, validating = true)),
                cordappsForAllNodes = cordapps
            )
        ) {
            val alice = startNode(providedName = ALICE_NAME, rpcUsers = listOf(user)).getOrThrow()
            assertThrows<NotaryException> {
                alice.rpc.startFlow(::ConflictFlow).returnValue.getOrThrow(20.seconds)
            }
        }
    }

    @Test(timeout = 300_000)
    fun `validating notary time window invalid error causes the flow to fail`() {
        driver(
            DriverParameters(
                startNodesInProcess = true,
                notarySpecs = listOf(NotarySpec(DUMMY_NOTARY_NAME, validating = true)),
                cordappsForAllNodes = cordapps
            )
        ) {
            val alice = startNode(providedName = ALICE_NAME, rpcUsers = listOf(user)).getOrThrow()
            assertThrows<NotaryException> {
                alice.rpc.startFlow(::InvalidTimeWindowFlow).returnValue.getOrThrow(20.seconds)
            }
        }
    }

    @Test(timeout = 300_000)
    fun `validating notary verification error causes the flow to fail`() {
        driver(
            DriverParameters(
                startNodesInProcess = true,
                notarySpecs = listOf(NotarySpec(DUMMY_NOTARY_NAME, validating = true)),
                cordappsForAllNodes = cordapps
            )
        ) {
            val alice = startNode(providedName = ALICE_NAME, rpcUsers = listOf(user)).getOrThrow()
            assertThrows<NotaryException> {
                alice.rpc.startFlow(::InvalidTransactionFlow).returnValue.getOrThrow(20.seconds)
            }
        }
    }

    @StartableByRPC
    class ConflictFlow : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first()).apply {
                addOutputState(DummyState())
                addCommand(DummyContract.Commands.Create(), serviceHub.myInfo.singleIdentity().owningKey)
            }
            val tx = serviceHub.signInitialTransaction(builder)
            val ftx = subFlow(FinalityFlow(tx, emptyList()))

            val input = ftx.coreTransaction.outRef<DummyState>(0)

            val builder2 = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first()).apply {
                addInputState(input)
                addCommand(DummyContract.Commands.Move(), serviceHub.myInfo.singleIdentity().owningKey)
            }
            val tx2 = serviceHub.signInitialTransaction(builder2)
            subFlow(FinalityFlow(tx2, emptyList()))

            val builder3 = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first()).apply {
                addInputState(input)
                addCommand(DummyContract.Commands.Move(), serviceHub.myInfo.singleIdentity().owningKey)
            }
            val tx3 = serviceHub.signInitialTransaction(builder3)
            // double spend here
            subFlow(FinalityFlow(tx3, emptyList()))
        }
    }

    @StartableByRPC
    class InvalidTimeWindowFlow : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first()).apply {
                addOutputState(DummyState())
                setTimeWindow(TimeWindow.untilOnly(Instant.now().minusMillis(10.days.toMillis())))
                addCommand(DummyContract.Commands.Create(), serviceHub.myInfo.singleIdentity().owningKey)
            }
            val tx = serviceHub.signInitialTransaction(builder)
            subFlow(FinalityFlow(tx, emptyList()))
        }
    }

    @StartableByRPC
    class InvalidTransactionFlow : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first()).apply {
                addOutputState(MyState())
                addCommand(DummyContract.Commands.Create(), serviceHub.myInfo.singleIdentity().owningKey)
            }
            val tx = serviceHub.signInitialTransaction(builder)
            subFlow(NotaryFlow.Client(tx, skipVerification = true))
        }
    }

    @BelongsToContract(MyContract::class)
    data class MyState @JvmOverloads constructor(
        override val participants: List<AbstractParty> = listOf()
    ) : ContractState {
    }

    class MyContract : Contract {

        override fun verify(tx: LedgerTransaction) {
            throw IllegalArgumentException("breaks")
        }
    }
}