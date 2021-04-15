package net.corda.networkcloner.test

import net.corda.core.internal.createComponentGroups
import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.api.IdentityMapper
import net.corda.networkcloner.impl.SerializerImpl
import net.corda.networkcloner.impl.TransactionsStoreImpl
import net.corda.networkcloner.impl.TxEditorImpl
import org.junit.Ignore
import org.junit.Test
import java.nio.file.Paths

class TxEditorTests : TestSupport() {

    @Test
    fun `Public key in a transaction can be replaced`() {
        val pathToTestDb = TxEditorTests::class.java.getResource("/snapshots/s1/source/persistence.mv.db").path.removeSuffix(".mv.db")
        val transactionsStore = TransactionsStoreImpl("jdbc:h2:$pathToTestDb","sa","")
        val sourceTxByteArray = transactionsStore.getAllTransactions().first()

        val serializer = getSerializer("s1")
        val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceTxByteArray)
        val sourceWireTransaction = sourceSignedTransaction.coreTransaction as WireTransaction

        val identityMapper = getIdentityMapper("s1")
        val clientSourcePublicKey = identityMapper.getSourceIdentity(clientX500Name)?.identityKey?.public ?: throw AssertionError("Expected to find $clientX500Name source identity public key")
        val clientDestinationPublicKey = identityMapper.getDestinationIdentity(clientX500Name)?.identityKey?.public ?: throw AssertionError("Expected to find $clientX500Name destination identity public key")

        val txEditor = getTxEditor()
        //txEditor.replacePublicKey(sourceWireTransaction.componentGroups)

        val destComponentGroups = with(sourceWireTransaction) {
            createComponentGroups(inputs, outputs, commands, attachments, notary, timeWindow, references, networkParametersHash)
        }


    }

}