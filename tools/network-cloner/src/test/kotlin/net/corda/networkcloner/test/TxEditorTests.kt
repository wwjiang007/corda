package net.corda.networkcloner.test

import net.corda.networkcloner.impl.txeditors.TxCommandsEditor
import net.corda.networkcloner.util.IdentityFactory
import net.corda.networkcloner.util.toTransactionComponents
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TxEditorTests : TestSupport() {

    @Test
    fun `Test app TxEditor can be loaded and applied`() {
        val nodeDatabase = getNodeDatabase("s1","source","client")
        val sourceTxByteArray = nodeDatabase.readMigrationData().transactions.first().transaction

        val serializer = getSerializer("s1")
        val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceTxByteArray)

        val sourcePartyRepository = getPartyRepository("s1","source")
        val destPartyRepository = getPartyRepository("s1", "destination")
        val identities = IdentityFactory.getIdentities(sourcePartyRepository, destPartyRepository)

        val cordappRepository = getCordappsRepository("s1")
        val txEditors = cordappRepository.getTxEditors()
        assertEquals(1, txEditors.size)
        val txEditor = txEditors.single()
        val transactionComponents = sourceSignedTransaction.toTransactionComponents()

        val editedTransactionComponents = txEditor.edit(transactionComponents, identities)

        assertTrue(editedTransactionComponents.outputs.all {
            it.data.participants.intersect(identities.map { it.sourceParty }).isEmpty()
        }, "All outputs should be clear of any original (source) party in their participants list")

        assertTrue(editedTransactionComponents.outputs.all {
            it.data.participants.intersect(identities.map { it.destinationPartyAndPrivateKey.party }).isNotEmpty()
        }, "All outputs should have at least one participant from the destination list of parties")
    }

    @Test
    fun `Commands TxEditor can be applied and works`() {
        val nodeDatabase = getNodeDatabase("s1","source","client")
        val sourceTxByteArray = nodeDatabase.readMigrationData().transactions.first().transaction

        val serializer = getSerializer("s1")
        val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceTxByteArray)

        val sourcePartyRepository = getPartyRepository("s1","source")
        val destPartyRepository = getPartyRepository("s1", "destination")
        val identities = IdentityFactory.getIdentities(sourcePartyRepository, destPartyRepository)

        val txCommandsEditor = TxCommandsEditor()
        val transactionComponents = sourceSignedTransaction.toTransactionComponents()

        val editedTransactionComponents = txCommandsEditor.edit(transactionComponents, identities)

        assertTrue(editedTransactionComponents.commands.all {
            it.signers.intersect(identities.map { it.sourceParty.owningKey }).isEmpty()
        }, "All commands signers should be clear of any original (source) party owning keys")

        assertTrue(editedTransactionComponents.commands.all {
            it.signers.intersect(identities.map { it.destinationPartyAndPrivateKey.party.owningKey }).isNotEmpty()
        }, "All commands signers should have at least one signer from the destination list of owning keys")
    }
}