package net.corda.networkcloner.util

import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.node.internal.DBNetworkParametersStorage
import net.corda.node.services.persistence.DBTransactionStorage
import net.corda.node.services.persistence.NodeAttachmentService
import net.corda.node.services.vault.VaultSchemaV1
import org.h2.jdbcx.JdbcDataSource
import org.hibernate.cfg.AvailableSettings
import org.hibernate.jpa.HibernatePersistenceProvider
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor
import java.net.URL
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.SharedCacheMode
import javax.persistence.ValidationMode
import javax.persistence.spi.ClassTransformer
import javax.persistence.spi.PersistenceUnitInfo
import javax.persistence.spi.PersistenceUnitTransactionType
import javax.sql.DataSource

class JpaEntityManagerFactory(val dbUrl: String, val dbUserName: String, val dbPassword: String, private val wellKnownPartyFromX500Name: (CordaX500Name) -> Party?, private val wellKnownPartyFromAnonymous: (AbstractParty) -> Party?, val additionalManagedClasses : List<Class<*>>, private val additionalClassLoaders: List<ClassLoader>) {
    val entityManager: EntityManager
        get() = entityManagerFactory.createEntityManager()

    private val entityManagerFactory: EntityManagerFactory
        get() {
            val persistenceUnitInfo: PersistenceUnitInfo = getPersistenceUnitInfo(UUID.randomUUID().toString())
            val configuration: Map<String?, Any?> = mapOf("hibernate.metadata_builder_contributor" to AttributeConverterMetadataBuilderContributor(wellKnownPartyFromX500Name, wellKnownPartyFromAnonymous),
                                                          AvailableSettings.CLASSLOADERS to additionalClassLoaders)
            return EntityManagerFactoryBuilderImpl(
                    PersistenceUnitInfoDescriptor(persistenceUnitInfo), configuration)
                    .build()
        }

    private fun getPersistenceUnitInfo(name: String): HibernatePersistenceUnitInfo {
        return HibernatePersistenceUnitInfo(name, getEntityClassNames(), getProperties())
    }

    private fun getEntityClassNames(): List<String> {
        return getEntities().map { it.name }
    }

    private fun getProperties(): Properties {
        val properties = Properties()
        properties["hibernate.dialect"] = "org.hibernate.dialect.H2Dialect"
        properties["hibernate.id.new_generator_mappings"] = false
        properties["hibernate.connection.datasource"] = getDataSource()
        return properties
    }

    private fun getEntities(): List<Class<*>> {
        return listOf(DBTransactionStorage.DBTransaction::class.java,
                      DBNetworkParametersStorage.PersistentNetworkParameters::class.java,
                      VaultSchemaV1.PersistentParty::class.java,
                      VaultSchemaV1.VaultLinearStates::class.java,
                      VaultSchemaV1.VaultStates::class.java,
                      NodeAttachmentService.DBAttachment::class.java) + additionalManagedClasses
    }

    private fun getDataSource(): DataSource {
        val dataSource = JdbcDataSource()
        dataSource.setURL(dbUrl)
        dataSource.setUser(dbUserName)
        dataSource.setPassword(dbPassword)
        return dataSource
    }

    class HibernatePersistenceUnitInfo(private val persistenceUnitName: String, private val managedClassNames: List<String>, private val properties: Properties) : PersistenceUnitInfo {
        private val transformers: MutableList<ClassTransformer?> = mutableListOf()
        private var jtaDataSource: DataSource? = null
        private var nonjtaDataSource: DataSource? = null
        private var transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL

        override fun getPersistenceUnitName(): String = persistenceUnitName

        override fun getPersistenceProviderClassName(): String = HibernatePersistenceProvider::class.java.name

        override fun getTransactionType(): PersistenceUnitTransactionType = transactionType

        fun setJtaDataSource(jtaDataSource: DataSource?): HibernatePersistenceUnitInfo? {
            this.jtaDataSource = jtaDataSource
            this.nonjtaDataSource = null
            transactionType = PersistenceUnitTransactionType.JTA
            return this
        }

        override fun getJtaDataSource(): DataSource? {
            return jtaDataSource
        }

        fun setNonJtaDataSource(nonJtaDataSource: DataSource): HibernatePersistenceUnitInfo? {
            this.nonjtaDataSource = nonJtaDataSource
            this.jtaDataSource = null
            transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL
            return this
        }

        override fun getNonJtaDataSource(): DataSource? {
            return nonjtaDataSource
        }

        override fun getMappingFileNames(): MutableList<String> = mutableListOf()

        override fun getJarFileUrls(): MutableList<URL> = mutableListOf()

        override fun getPersistenceUnitRootUrl(): URL? = null

        override fun getManagedClassNames(): MutableList<String> = managedClassNames.toMutableList()

        override fun excludeUnlistedClasses(): Boolean = false

        override fun getSharedCacheMode(): SharedCacheMode = SharedCacheMode.UNSPECIFIED

        override fun getValidationMode(): ValidationMode = ValidationMode.AUTO

        override fun getProperties(): Properties = properties

        override fun getPersistenceXMLSchemaVersion(): String = "2.1"

        override fun getClassLoader(): ClassLoader = Thread.currentThread().contextClassLoader

        override fun addTransformer(transformer: ClassTransformer?) {
            transformers.add(transformer)
        }

        override fun getNewTempClassLoader(): ClassLoader? = null
    }
}

