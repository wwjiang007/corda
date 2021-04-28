package net.corda.networkcloner.impl

import net.corda.core.cloning.TxEditor
import net.corda.networkcloner.api.TxEditorFactory
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile

class DirectoryBasedTxEditorFactory(val directory : File) : TxEditorFactory {

    private val _txEditors = loadTxEditors()

    private fun loadTxEditors() : List<TxEditor> {
        return directory.listFiles().filter { it.isFile && it.name.endsWith(".jar",true) }.flatMap {
            getClassesFromJarFile(it)
        }.filter {
            TxEditor::class.java.isAssignableFrom(it)
        }.map { it.newInstance() as TxEditor }
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

    override fun getTxEditors(): List<TxEditor> {
        return _txEditors
    }
}