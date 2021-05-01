package net.corda.networkcloner.impl

import net.corda.core.crypto.SecureHash
import net.corda.networkcloner.api.NodeDatabase
import net.corda.networkcloner.entity.MigrationData
import net.corda.networkcloner.util.JpaEntityManagerFactory
import net.corda.node.internal.DBNetworkParametersStorage
import net.corda.node.services.persistence.DBTransactionStorage
import javax.persistence.EntityManager

class NodeDatabaseImpl(url : String, username: String, password: String) : NodeDatabase {

    private val entityManager : EntityManager = JpaEntityManagerFactory(url, username, password).entityManager

    override fun readMigrationData(): MigrationData {
        val transactions = getTransactions()
        return MigrationData(transactions, emptyList(), emptyList(), emptyList())
    }

    override fun writeMigrationData(migrationData: MigrationData) {
        entityManager.transaction.begin()
        migrationData.transactions.forEach {
            entityManager.persist(it)
        }
        entityManager.transaction.commit()
    }

    override fun readNetworkParametersHash(): SecureHash {
        val query = entityManager.createQuery("SELECT e FROM DBNetworkParametersStorage\$PersistentNetworkParameters e")
        return SecureHash.parse((query.resultList.single() as DBNetworkParametersStorage.PersistentNetworkParameters).hash)
    }

    private fun getTransactions(): List<DBTransactionStorage.DBTransaction> {
        val query = entityManager.createQuery("SELECT e FROM DBTransactionStorage\$DBTransaction e")
        @Suppress("UNCHECKED_CAST")
        return query.resultList as List<DBTransactionStorage.DBTransaction>
    }


}