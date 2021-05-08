package net.corda.networkcloner.impl

import net.corda.core.cloning.TxEditor
import net.corda.networkcloner.api.CordappsRepository
import net.corda.node.VersionInfo
import net.corda.node.internal.cordapp.JarScanningCordappLoader
import net.corda.nodeapi.internal.cordapp.CordappLoader
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths
import java.util.jar.JarFile

class CordappsRepositoryImpl(private val pathToCordapps : File) : CordappsRepository {

    private val _cordappLoader = createCordappLoader(pathToCordapps)
    private val _txEditors = loadTxEditors()

    override fun getCordappLoader(): CordappLoader {
        return _cordappLoader
    }

    override fun getTxEditors(): List<TxEditor> {
        return _txEditors
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
        val allClassesFromTempClassLoader = pathToCordapps.listFiles().filter { it.isFile && it.name.endsWith(".jar",true) }.flatMap {
            getClassesFromJarFile(it)
        }

        val txEditorClassesFromTempClassLoader = allClassesFromTempClassLoader.filter { TxEditor::class.java.isAssignableFrom(it) }

        val txEditorClasses = txEditorClassesFromTempClassLoader.map {
            _cordappLoader.appClassLoader.loadClass(it.name)
        }

        return txEditorClasses.map { it.newInstance() as TxEditor }
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