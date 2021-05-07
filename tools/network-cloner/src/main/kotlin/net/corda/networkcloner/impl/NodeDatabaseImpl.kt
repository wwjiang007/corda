package net.corda.networkcloner.impl

import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.networkcloner.api.NodeDatabase
import net.corda.networkcloner.entity.MigrationData
import net.corda.networkcloner.util.JpaEntityManagerFactory
import net.corda.node.internal.DBNetworkParametersStorage
import net.corda.node.services.persistence.DBTransactionStorage
import net.corda.node.services.vault.VaultSchemaV1
import javax.persistence.EntityManager

class NodeDatabaseImpl(url : String, username: String, password: String, wellKnownPartyFromX500Name: (CordaX500Name) -> Party?, wellKnownPartyFromAnonymous: (AbstractParty) -> Party?) : NodeDatabase {

    private val entityManager : EntityManager = JpaEntityManagerFactory(url, username, password, wellKnownPartyFromX500Name, wellKnownPartyFromAnonymous).entityManager

    override fun readMigrationData(): MigrationData {
        val transactions = getTransactions()
        val persistentParties = getPersistentParties()
        val vaultLinearStates = getVaultLinearStates()
        val vaultStates = getVaultStates()
        return MigrationData(transactions, persistentParties, vaultLinearStates, vaultStates)
    }

    override fun writeMigrationData(migrationData: MigrationData) {
        entityManager.transaction.begin()
        migrationData.transactions.forEach {
            entityManager.persist(it)
        }
        migrationData.persistentParties.forEach {
            entityManager.persist(it)
        }
        migrationData.vaultLinearStates.forEach {
            entityManager.persist(it)
        }
        migrationData.vaultStates.forEach {
            entityManager.persist(it)
        }
        entityManager.transaction.commit()
    }

    override fun readNetworkParametersHash(): SecureHash {
        val query = entityManager.createQuery("SELECT e FROM DBNetworkParametersStorage\$PersistentNetworkParameters e")
        return SecureHash.parse((query.resultList.single() as DBNetworkParametersStorage.PersistentNetworkParameters).hash)
    }

    private fun getVaultStates(): List<VaultSchemaV1.VaultStates> {
        val query = entityManager.createQuery("SELECT e FROM VaultSchemaV1\$VaultStates e")
        @Suppress("UNCHECKED_CAST")
        return query.resultList as List<VaultSchemaV1.VaultStates>
    }

    private fun getVaultLinearStates(): List<VaultSchemaV1.VaultLinearStates> {
        val query = entityManager.createQuery("SELECT e FROM VaultSchemaV1\$VaultLinearStates e")
        @Suppress("UNCHECKED_CAST")
        return query.resultList as List<VaultSchemaV1.VaultLinearStates>
    }

    private fun getPersistentParties(): List<VaultSchemaV1.PersistentParty> {
        val query = entityManager.createQuery("SELECT e FROM VaultSchemaV1\$PersistentParty e")
        @Suppress("UNCHECKED_CAST")
        return query.resultList as List<VaultSchemaV1.PersistentParty>
    }

    private fun getTransactions(): List<DBTransactionStorage.DBTransaction> {
        val query = entityManager.createQuery("SELECT e FROM DBTransactionStorage\$DBTransaction e")
        @Suppress("UNCHECKED_CAST")
        return query.resultList as List<DBTransactionStorage.DBTransaction>
    }


}