package net.corda.networkcloner.impl

import net.corda.core.cloning.MigrationContext
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.networkcloner.api.MigrationTaskFactory
import net.corda.networkcloner.api.NodeDatabase
import net.corda.networkcloner.entity.MigrationTask
import java.io.File
import java.lang.RuntimeException

class NodesToNodesMigrationTaskFactory(val source : File, val destination : File) : MigrationTaskFactory {

    override fun getMigrationTasks() : List<MigrationTask> {
        val sourcePartiesRepo = NodesDirPartyRepository(source)
        val destinationPartiesRepo = NodesDirPartyRepository(destination)
        val identitySpace = IdentitySpaceImpl(sourcePartiesRepo, destinationPartiesRepo)
        val identities = identitySpace.getIdentities()

        val sourceTransactionStores = getTransactionStores(source, identitySpace::getSourcePartyFromX500Name, identitySpace::getSourcePartyFromAnonymous)
        val destinationTransactionStores = getTransactionStores(destination, identitySpace::getDestinationPartyFromX500Name, identitySpace::getDestinationPartyFromAnonymous)

        val sourceNetworkParametersHash = sourceTransactionStores.values.map { it.readNetworkParametersHash() }.toSet().single()
        val destinationNetworkParametershash = destinationTransactionStores.values.map { it.readNetworkParametersHash() }.toSet().single()

        return identities.map { identity ->
            val sourceTransactionsStore = sourceTransactionStores[identity.sourceParty.name] ?: throw RuntimeException("Expected to find source transactions store for identity $identity")
            val destinationTransactionsStore = destinationTransactionStores[identity.destinationPartyAndPrivateKey.party.name] ?: throw RuntimeException("Expected to find destination transactions store for identity $identity")
            MigrationTask(identity, sourceTransactionsStore, destinationTransactionsStore, MigrationContext(identitySpace, sourceNetworkParametersHash, destinationNetworkParametershash, emptyMap()))
        }
    }

    private fun getTransactionStores(nodesDir : File, wellKnownPartyFromX500Name: (CordaX500Name) -> Party?, wellKnownPartyFromAnonymous: (AbstractParty) -> Party?) : Map<CordaX500Name, NodeDatabase> {
        return nodesDir.listFiles().filter { it.isDirectory && !it.isHidden }.map {
            val nodeConf = File(it, "node.conf").also {
                if (!it.exists()) {
                    throw RuntimeException("Expected $it to exist")
                }
            }
            val myLegalNameLine = nodeConf.readLines().find { it.contains("myLegalName") }
            val legalName = myLegalNameLine!!.substring(myLegalNameLine.indexOf("\"")+1, myLegalNameLine.lastIndexOf("\""))
            val x500Name = CordaX500Name.parse(legalName)
            val pathToDb = File(it, "persistence.mv.db").also {
                if (!it.exists()) {
                    throw RuntimeException("Expected $it to exist")
                }
            }.path.removeSuffix(".mv.db")
            val transactionStore = NodeDatabaseImpl("jdbc:h2:$pathToDb", "sa","", wellKnownPartyFromX500Name, wellKnownPartyFromAnonymous)
            x500Name to transactionStore
        }.toMap()
    }
}