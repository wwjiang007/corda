package net.corda.networkcloner.test

import net.corda.core.cloning.MigrationContext
import net.corda.core.crypto.SecureHash
import net.corda.networkcloner.impl.IdentitySpaceImpl
import net.corda.networkcloner.impl.txeditors.TxCommandsEditor
import net.corda.networkcloner.impl.txeditors.TxNetworkParametersHashEditor
import net.corda.networkcloner.impl.txeditors.TxNotaryEditor
import net.corda.networkcloner.util.toTransactionComponents
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
        val identitySpace = IdentitySpaceImpl(sourcePartyRepository, destPartyRepository)
        val identities = identitySpace.getIdentities()

        val cordappRepository = getCordappsRepository("s1")
        val txEditors = cordappRepository.getTxEditors()
        assertEquals(1, txEditors.size)
        val txEditor = txEditors.single()
        val transactionComponents = sourceSignedTransaction.toTransactionComponents()

        val editedTransactionComponents = txEditor.edit(transactionComponents, MigrationContext(identitySpace, SecureHash.zeroHash, SecureHash.allOnesHash))

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
        val identitySpace = IdentitySpaceImpl(sourcePartyRepository, destPartyRepository)
        val identities = identitySpace.getIdentities()

        val txCommandsEditor = TxCommandsEditor()
        val transactionComponents = sourceSignedTransaction.toTransactionComponents()

        val editedTransactionComponents = txCommandsEditor.edit(transactionComponents, MigrationContext(identitySpace, SecureHash.zeroHash, SecureHash.allOnesHash))

        assertTrue(editedTransactionComponents.commands.all {
            it.signers.intersect(identities.map { it.sourceParty.owningKey }).isEmpty()
        }, "All commands signers should be clear of any original (source) party owning keys")

        assertTrue(editedTransactionComponents.commands.all {
            it.signers.intersect(identities.map { it.destinationPartyAndPrivateKey.party.owningKey }).isNotEmpty()
        }, "All commands signers should have at least one signer from the destination list of owning keys")
    }

    @Test
    fun `Notary TxEditor can be applied and works`() {
        val nodeDatabase = getNodeDatabase("s1","source","client")
        val sourceTxByteArray = nodeDatabase.readMigrationData().transactions.first().transaction

        val serializer = getSerializer("s1")
        val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceTxByteArray)

        val sourcePartyRepository = getPartyRepository("s1","source")
        val destPartyRepository = getPartyRepository("s1", "destination")
        val identitySpace = IdentitySpaceImpl(sourcePartyRepository, destPartyRepository)

        val txNotaryEditor = TxNotaryEditor()
        val transactionComponents = sourceSignedTransaction.toTransactionComponents()

        val editedTransactionComponents = txNotaryEditor.edit(transactionComponents, MigrationContext(identitySpace, SecureHash.zeroHash, SecureHash.allOnesHash))

        val expectedNotary = destPartyRepository.getParties().find { it.name.toString().contains("Notary", true) }
        assertNotNull(expectedNotary)
        assertEquals(expectedNotary, editedTransactionComponents.notary)
    }

    @Test
    fun `Network Parameters Hash TxEditor can be applied and works`() {
        val nodeDatabase = getNodeDatabase("s1","source","client")
        val sourceNetworksParametersHash = nodeDatabase.readNetworkParametersHash()
        val sourceTxByteArray = nodeDatabase.readMigrationData().transactions.first().transaction

        val serializer = getSerializer("s1")
        val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceTxByteArray)

        val sourcePartyRepository = getPartyRepository("s1","source")
        val destPartyRepository = getPartyRepository("s1", "destination")
        val identitySpace = IdentitySpaceImpl(sourcePartyRepository, destPartyRepository)
        val identities = identitySpace.getIdentities()

        val txNetworkParametersHashEditor = TxNetworkParametersHashEditor()
        val transactionComponents = sourceSignedTransaction.toTransactionComponents()

        val editedTransactionComponents = txNetworkParametersHashEditor.edit(transactionComponents, MigrationContext(identitySpace, sourceNetworksParametersHash, SecureHash.allOnesHash))

        assertEquals(SecureHash.allOnesHash, editedTransactionComponents.networkParametersHash)
    }
}