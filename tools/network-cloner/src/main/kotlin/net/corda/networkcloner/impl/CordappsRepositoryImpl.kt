package net.corda.networkcloner.impl

import net.corda.core.cloning.EntityMigration
import net.corda.core.cloning.TxEditor
import net.corda.networkcloner.FailedAssumptionException
import net.corda.networkcloner.api.CordappsRepository
import net.corda.node.VersionInfo
import net.corda.node.internal.cordapp.JarScanningCordappLoader
import net.corda.nodeapi.internal.cordapp.CordappLoader
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths
import java.util.jar.JarFile

class CordappsRepositoryImpl(private val pathToCordapps : File, private val expectedNumberOfTxEditors : Int, private val expectedNumberOfPersistentStateMigrations : Int) : CordappsRepository {

    private val _cordappLoader : CordappLoader
    private val _txEditors : List<TxEditor>
    private val _entityMigrations : List<EntityMigration<*>>
    private val _jarUrls : List<URL>

    init {
        verifyPathToCordapps()
        _cordappLoader = createCordappLoader(pathToCordapps)
        _txEditors = loadTxEditors()
        _entityMigrations = loadPersistentStateMigrations()
        _jarUrls = getJarFiles().map { it.toURI().toURL() }
    }

    private fun verifyPathToCordapps() {
        require(pathToCordapps.exists() && pathToCordapps.isDirectory) { "Directory $pathToCordapps either doesn't exist or is not a directory" }
    }

    override fun getCordappLoader(): CordappLoader {
        return _cordappLoader
    }

    override fun getTxEditors(): List<TxEditor> {
        return _txEditors
    }

    override fun getEntityMigrations(): List<EntityMigration<*>> {
        return _entityMigrations
    }

    override fun getCordappsURLs(): List<URL> {
        return _jarUrls
    }

    private fun createCordappLoader(directory: File) : CordappLoader {
        val pathToCordapps = directory.path
        return JarScanningCordappLoader.fromDirectories(
                listOf(Paths.get(pathToCordapps)),
                VersionInfo.UNKNOWN,
                extraCordapps = emptyList(),
                signerKeyFingerprintBlacklist = emptyList()
        )
    }

    private fun loadTxEditors() : List<TxEditor> {
        val allClassesFromTempClassLoader = getAllClassesViaTemporaryClassLoader()

        val txEditorClassesFromTempClassLoader = allClassesFromTempClassLoader.filter { TxEditor::class.java.isAssignableFrom(it) }

        val txEditorClasses = txEditorClassesFromTempClassLoader.map {
            _cordappLoader.appClassLoader.loadClass(it.name)
        }

        return txEditorClasses.map { it.newInstance() as TxEditor }.also {
            if (it.size != expectedNumberOfTxEditors) {
                throw FailedAssumptionException("Expected to find $expectedNumberOfTxEditors transaction editors in the cordapps, found ${it.size}")
            }
        }
    }

    private fun loadPersistentStateMigrations() : List<EntityMigration<*>> {
        val allClassesFromTempClassLoader = getAllClassesViaTemporaryClassLoader()

        val persistentStateMigrationClassesFromTempClassLoader = allClassesFromTempClassLoader.filter { EntityMigration::class.java.isAssignableFrom(it) }

        val persistentStateMigrationClasses = persistentStateMigrationClassesFromTempClassLoader.map {
            _cordappLoader.appClassLoader.loadClass(it.name)
        }

        return persistentStateMigrationClasses.map { it.newInstance() as EntityMigration<*> }.also {
            if (it.size != expectedNumberOfPersistentStateMigrations) {
                throw FailedAssumptionException("Expected to find $expectedNumberOfPersistentStateMigrations persistent state migrations in the cordapps, found ${it.size}")
            }
        }
    }

    private fun getAllClassesViaTemporaryClassLoader() : List<Class<*>> {
        return getJarFiles().flatMap {
            getClassesFromJarFile(it)
        }
    }

    private fun getJarFiles() : List<File> {
        return pathToCordapps.listFiles().filter { it.isFile && it.name.endsWith(".jar",true) }
    }

    private fun getClassNamesFromJarFile(givenFile: File): Set<String> {
        val classNames = mutableSetOf<String>()
        JarFile(givenFile).use { jarFile ->
            val e = jarFile.entries()
            while (e.hasMoreElements()) {
                val jarEntry = e.nextElement()
                if (jarEntry.name.endsWith(".class")) {
                    val className = jarEntry.name
                            .replace("/", ".")
                            .replace(".class", "")
                    classNames.add(className)
                }
            }
            return classNames
        }
    }

    private fun getClassesFromJarFile(jarFile: File): Set<Class<*>> {
        val classNames = getClassNamesFromJarFile(jarFile)
        val classes = mutableSetOf<Class<*>>()
        URLClassLoader.newInstance(arrayOf(URL("jar:file:$jarFile!/"))).use { cl ->
            for (name in classNames) {
                val clazz = cl.loadClass(name)
                classes.add(clazz)
            }
        }
        return classes
    }

}