package net.corda.networkcloner.impl

import net.corda.core.identity.CordaX500Name
import net.corda.networkcloner.api.MigrationTaskFactory
import net.corda.networkcloner.api.NodeDatabase
import net.corda.networkcloner.entity.MigrationContext
import net.corda.networkcloner.entity.MigrationTask
import net.corda.networkcloner.util.IdentityFactory
import java.io.File
import java.lang.RuntimeException

class NodesToNodesMigrationTaskFactory(val source : File, val destination : File) : MigrationTaskFactory {

    override fun getMigrationTasks() : List<MigrationTask> {
        val sourcePartiesRepo = NodesDirPartyRepository(source)
        val destinationPartiesRepo = NodesDirPartyRepository(destination)
        val identities = IdentityFactory.getIdentities(sourcePartiesRepo, destinationPartiesRepo)

        val sourceTransactionStores = getTransactionStores(source)
        val destinationTransactionStores = getTransactionStores(destination)

        return identities.map { identity ->
            val sourceTransactionsStore = sourceTransactionStores[identity.sourceParty.name] ?: throw RuntimeException("Expected to find source transactions store for identity $identity")
            val destinationTransactionsStore = destinationTransactionStores[identity.destinationPartyAndPrivateKey.party.name] ?: throw RuntimeException("Expected to find destination transactions store for identity $identity")
            MigrationTask(identity, sourceTransactionsStore, destinationTransactionsStore, MigrationContext(identities))
        }
    }

    private fun getTransactionStores(nodesDir : File) : Map<CordaX500Name, NodeDatabase> {
        return nodesDir.listFiles().filter { it.isDirectory }.map {
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
            val transactionStore = NodeDatabaseImpl("jdbc:h2:$pathToDb", "sa","")
            x500Name to transactionStore
        }.toMap()
    }
}