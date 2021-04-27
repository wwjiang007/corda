package net.corda.networkcloner.entity

import net.corda.node.services.persistence.DBTransactionStorage
import net.corda.node.services.vault.VaultSchemaV1

data class MigrationData(val transactions : List<DBTransactionStorage.DBTransaction>, val persistentParties: List<VaultSchemaV1.PersistentParty>,
                    val vaultLinearStates: List<VaultSchemaV1.VaultLinearStates>, val vaultStates: List<VaultSchemaV1.VaultStates>)
