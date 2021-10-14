package net.corda.coretests.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.PartyAndCertificate
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.testing.contracts.DummyContract
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import net.corda.testing.core.CHARLIE_NAME
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockServices
import net.corda.testing.node.internal.DUMMY_CONTRACTS_CORDAPP
import net.corda.testing.node.internal.InternalMockNetwork
import net.corda.testing.node.internal.enclosedCordapp
import org.hamcrest.CoreMatchers.`is`
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Test
import java.security.PublicKey
import java.time.Instant
import java.util.function.Predicate

class SignatureValidatinNotaryTest : WithContracts {
    companion object {
        private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "London", "GB"))
        private val miniCorpServices = MockServices(listOf("net.corda.testing.contracts"), miniCorp)
        private val classMockNet = InternalMockNetwork(
                cordappsForAllNodes = listOf(DUMMY_CONTRACTS_CORDAPP, enclosedCordapp()),
                notarySpecs = listOf(MockNetworkNotarySpec(DUMMY_NOTARY_NAME, false))
        )

        private const val MAGIC_NUMBER = 1337

        @JvmStatic
        @AfterClass
        fun tearDown() = classMockNet.stopNodes()
    }

    override val mockNet = classMockNet

    private val aliceNode = makeNode(ALICE_NAME)
    private val bobNode = makeNode(BOB_NAME)
    private val charlieNode = makeNode(CHARLIE_NAME)

    private val alice = aliceNode.info.singleIdentity()
    private val bob = bobNode.info.singleIdentity()
    private val charlie = charlieNode.info.singleIdentity()

    @Test(timeout=300_000)
	fun `successfully collects signatures when sessions are initiated with AnonymousParty`() {
        val aConfidentialIdentity1 = aliceNode.createConfidentialIdentity(alice)
        val bConfidentialIdentity1 = bobNode.createConfidentialIdentity(bob)
        val bConfidentialIdentity2 = bobNode.createConfidentialIdentity(bob)
        val cConfidentialIdentity1 = charlieNode.createConfidentialIdentity(charlie)

        bobNode.registerInitiatedFlow(NotarisationExploratoryTestFlowResponder::class.java)
        charlieNode.registerInitiatedFlow(NotarisationExploratoryTestFlowResponder::class.java)

        val owners = listOf(aConfidentialIdentity1, bConfidentialIdentity1, bConfidentialIdentity2, cConfidentialIdentity1)

        val stx = aliceNode.startFlow(NotarisationExploratoryTestFlow(owners)).also { mockNet.runNetwork() }.resultFuture.get()
    }

    @Test(timeout=300_000)
    fun `non-validating notary refuse to finalise when a required signer is missing`() {
        val aConfidentialIdentity1 = aliceNode.createConfidentialIdentity(alice)
        val bConfidentialIdentity1 = bobNode.createConfidentialIdentity(bob)
        val bConfidentialIdentity2 = bobNode.createConfidentialIdentity(bob)
        val cConfidentialIdentity1 = charlieNode.createConfidentialIdentity(charlie)

        bobNode.registerInitiatedFlow(CheatingNotarisationFlowResponder::class.java)
        charlieNode.registerInitiatedFlow(CheatingNotarisationFlowResponder::class.java)

        val owners = listOf(aConfidentialIdentity1, bConfidentialIdentity1, bConfidentialIdentity2, cConfidentialIdentity1)

        val stx = aliceNode.startFlow(CheatingNotarisationFlow(owners)).also { mockNet.runNetwork() }.resultFuture.get()
    }

//    @Test(timeout=300_000)
//    fun `test filters`() {
//        val aConfidentialIdentity1 = aliceNode.createConfidentialIdentity(alice)
//        val bConfidentialIdentity1 = bobNode.createConfidentialIdentity(bob)
//        val bConfidentialIdentity2 = bobNode.createConfidentialIdentity(bob)
//        val cConfidentialIdentity1 = charlieNode.createConfidentialIdentity(charlie)
//        val owners = listOf(aConfidentialIdentity1, bConfidentialIdentity1, bConfidentialIdentity2, cConfidentialIdentity1)
//
//        val wtx = aliceNode.startFlow(DummyTestFlow(owners)).also { mockNet.runNetwork() }.resultFuture.get()
//
//        fun commands(elem: Any): Boolean {
//            return when (elem) {
//                is Command<*> -> true
//                else -> false
//            }
//        }
//
//        fun pubkeys(elem: Any): Boolean {
//            return when (elem) {
//                is PublicKey -> true
//                else -> false
//            }
//        }
//
//
//
//        val ftxCommands = wtx.buildFilteredTransaction(Predicate(::commands))
//
//
//        ftxCommands.checkAllComponentsVisible(ComponentGroupEnum.SIGNERS_GROUP)
//        ftxCommands.checkAllComponentsVisible(ComponentGroupEnum.COMMANDS_GROUP)
//        ftxCommands.checkCommandVisibility(owners[0].owningKey)
//
//        fun singleSigner(elem: Any) = elem is Command<*> && elem.signers.size == 1
//        val ftxCommand = wtx.buildFilteredTransaction(Predicate(::singleSigner))
//        ftxCommand.checkAllComponentsVisible(ComponentGroupEnum.SIGNERS_GROUP)
//        ftxCommand.checkAllComponentsVisible(ComponentGroupEnum.COMMANDS_GROUP) // Throws!
//        ftxCommand.checkCommandVisibility(owners[0].owningKey)  // Throws!
//
//        val ftxPubKeys = wtx.buildFilteredTransaction(Predicate(::pubkeys))
//
//    }

}

@InitiatingFlow
class NotarisationExploratoryTestFlow(private val cis: List<PartyAndCertificate>) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        for (ci in cis) {
            if (ci.name != ourIdentity.name) {
                serviceHub.identityService.verifyAndRegisterIdentity(ci)
            }
        }
        val commandKeys = cis.map { it.owningKey }
        val state = DummyContract.MultiOwnerState(owners = cis.map { AnonymousParty(it.owningKey) })
        val create = DummyContract.Commands.Create()
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
                .addOutputState(state)
                .addCommand(create, commandKeys)
                // add time window to force notarisation with no inputs or references
                .setTimeWindow(TimeWindow.untilOnly(Instant.now().plusSeconds(3600)))

        val ourKey = cis.single { it.name == ourIdentity.name }.owningKey
        val signedByUsTx = serviceHub.signInitialTransaction(txBuilder, ourKey)
        val sessions = cis.filter { it.name != ourIdentity.name }.map { initiateFlow(AnonymousParty(it.owningKey)) }
        val sstx = subFlow(CollectSignaturesFlow(signedByUsTx, sessions, myOptionalKeys = listOf(ourKey)))
        val fstx = subFlow(FinalityFlow(sstx, sessions))



        return fstx
    }
}

@InitiatedBy(NotarisationExploratoryTestFlow::class)
class NotarisationExploratoryTestFlowResponder(private val otherSideSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signFlow = object : SignTransactionFlow(otherSideSession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val tx = stx.tx
                //val ltx = tx.toLedgerTransaction(serviceHub)
                "There should only be one output state" using (tx.outputs.size == 1)
                "There should only be one output state" using (tx.inputs.isEmpty())
                //val magicNumberState = ltx.outputsOfType<DummyContract.MultiOwnerState>().single()
                //"Must be $MAGIC_NUMBER or greater" using (magicNumberState.magicNumber >= MAGIC_NUMBER)
            }
        }
        val stx = subFlow(signFlow)
        subFlow(ReceiveFinalityFlow(otherSideSession, expectedTxId = stx.id))
    }
}

@InitiatingFlow
class CheatingNotarisationFlow(private val cis: List<PartyAndCertificate>) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        for (ci in cis) {
            if (ci.name != ourIdentity.name) {
                serviceHub.identityService.verifyAndRegisterIdentity(ci)
            }
        }
        val commandKeys = cis.map { it.owningKey }
        val state = DummyContract.MultiOwnerState(owners = cis.map { AnonymousParty(it.owningKey) })
        val create = DummyContract.Commands.Create()
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
                .addOutputState(state)
                .addCommand(create, commandKeys)
                // add time window to force notarisation with no inputs or references
                .setTimeWindow(TimeWindow.untilOnly(Instant.now().plusSeconds(3600)))

        val ourKey = cis.single { it.name == ourIdentity.name }.owningKey
        val signedByUsTx = serviceHub.signInitialTransaction(txBuilder, ourKey)
//        val sessions = cis.filter { it.name != ourIdentity.name }.map { initiateFlow(AnonymousParty(it.owningKey)) }
//        val sstx = subFlow(CollectSignaturesFlow(signedByUsTx, sessions, myOptionalKeys = listOf(ourKey)))
        val fstx = subFlow(FinalityFlow(signedByUsTx, listOf()))

        return fstx
    }
}

@InitiatedBy(CheatingNotarisationFlow::class)
class CheatingNotarisationFlowResponder(private val otherSideSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signFlow = object : SignTransactionFlow(otherSideSession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val tx = stx.tx
                //val ltx = tx.toLedgerTransaction(serviceHub)
                "There should only be one output state" using (tx.outputs.size == 1)
                "There should only be one output state" using (tx.inputs.isEmpty())
                //val magicNumberState = ltx.outputsOfType<DummyContract.MultiOwnerState>().single()
                //"Must be $MAGIC_NUMBER or greater" using (magicNumberState.magicNumber >= MAGIC_NUMBER)
            }
        }
        val stx = subFlow(signFlow)
        subFlow(ReceiveFinalityFlow(otherSideSession, expectedTxId = stx.id))
    }
}

@InitiatingFlow
class DummyTestFlow(private val cis: List<PartyAndCertificate>) : FlowLogic<WireTransaction>() {
    @Suspendable
    override fun call(): WireTransaction {

        for (ci in cis) {
            if (ci.name != ourIdentity.name) {
                serviceHub.identityService.verifyAndRegisterIdentity(ci)
            }
        }
        val state = DummyContract.MultiOwnerState(owners = cis.map { AnonymousParty(it.owningKey) })
        val create = DummyContract.Commands.Create()
        val move = DummyContract.Commands.Move()
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
                .addOutputState(state)
                .addCommand(create, cis.map { it.owningKey })
                .addCommand(move, cis.map { it.owningKey }.first())
                // add time window to force notarisation with no inputs or references
                .setTimeWindow(TimeWindow.untilOnly(Instant.now().plusSeconds(3600)))

        return txBuilder.toWireTransaction(serviceHub)
    }
}
