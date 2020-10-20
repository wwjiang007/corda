package net.corda.core.internal

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import kotlin.concurrent.thread

class NamedInputStream(private val inputStream: InputStream, val name: String) : InputStream() {
    override fun close() = inputStream.close()
    override fun read(): Int = inputStream.read()
    override fun read(b: ByteArray): Int = inputStream.read(b)
    override fun read(b: ByteArray, off: Int, len: Int): Int = inputStream.read(b, off, len)
    override fun skip(n: Long): Long = inputStream.skip(n)
    override fun available(): Int = inputStream.available()
    override fun mark(readlimit: Int) = inputStream.mark(readlimit)
    override fun reset() = inputStream.reset()
    override fun markSupported(): Boolean = inputStream.markSupported()
}

class JarOfJarsIterator(private val inputStream: InputStream) : Iterator<NamedInputStream> {
    private val jarInputStream = JarInputStream(inputStream)
    private var entry: JarEntry? = null

    override fun hasNext(): Boolean {
        entry = jarInputStream.nextJarEntry
        while (entry != null && !filter(entry!!)) {
            entry = jarInputStream.nextJarEntry
        }
        return entry != null
    }

    override fun next(): NamedInputStream {
        val jarEntry = entry
        if (jarEntry == null) throw IllegalStateException("Must call hasNext() first, or no more entries")
        entry = null
        println("Iterated to $jarEntry")
        val buffer = ByteArrayOutputStream()
        jarInputStream.copyTo(buffer)
        jarInputStream.closeEntry()
        return NamedInputStream(ByteArrayInputStream(buffer.toByteArray()), jarEntry.name)
    }

    protected fun filter(entry: JarEntry): Boolean {
        return entry.name.endsWith(".jar")
    }
}

class ConcatenatingJarInputStream(private val inputsStreams: Iterator<NamedInputStream>) : PipedInputStream(64 * 1024) {
    private val outputStream = PipedOutputStream()
    private val entries = HashSet<String>()

    init {
        connect(outputStream)
        thread(name = "$this-Thread", isDaemon = true) {
            var jarOutputStream: JarOutputStream? = null
            try {
                for (inputStream in inputsStreams) {
                    println("Processing input stream ${inputStream.name}")
                    val jarInputStream = JarInputStream(inputStream)
                    if (jarOutputStream == null) {
                        val manifest = jarInputStream.manifest
                        if (manifest == null) {
                            println("No manifest")
                            jarOutputStream = JarOutputStream(outputStream)
                        } else {
                            println("Manifest $manifest")
                            jarOutputStream = JarOutputStream(outputStream, manifest)
                        }
                    }
                    copyJar(jarOutputStream, jarInputStream, inputStream.name)
                }
                jarOutputStream?.close()
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                // We should close the input streams too, but not expecting a real runtime scenario, just during tests
                outputStream.close()
            }
        }
    }

    private fun copyJar(jarOutputStream: JarOutputStream, jarInputStream: JarInputStream, jarName: String) {
        var jarEntry = jarInputStream.nextJarEntry
        while (jarEntry != null) {
            if (entries.add(jarEntry.name)) {
                if (!skipOrSubstitute(jarOutputStream, jarEntry, jarName)) {
                    val size = jarEntry.size
                    copyEntry(jarOutputStream, jarEntry, jarInputStream, jarName, size)
                }
            } else if (!jarEntry.isDirectory) {
                println("Skipping duplicate entry ${jarEntry.name} in $jarName")
            }
            jarInputStream.closeEntry()
            jarEntry = jarInputStream.nextJarEntry
        }
        jarOutputStream.flush()
        jarInputStream.close()
    }

    protected fun skipOrSubstitute(jarOutputStream: JarOutputStream, jarEntry: JarEntry, jarName: String): Boolean {
        return false
    }

    private fun copyEntry(jarOutputStream: JarOutputStream, jarEntry: JarEntry, jarInputStream: JarInputStream, jarName: String, size: Long) {
        println("Copy entry ${jarEntry.name} from $jarName of size $size")
        jarOutputStream.putNextEntry(jarEntry)
        jarInputStream.copyTo(jarOutputStream)
        jarOutputStream.closeEntry()
    }
}

fun main(args: Array<String>) {
    //val inputsList = args.toMutableList().drop(1)
    val outputFilename = args[0]
    println("Output $outputFilename")
    //val inputStreams = inputsList.map { NamedInputStream(FileInputStream(it), it) }.iterator()
    val inputStreams = JarOfJarsIterator(FileInputStream(args[1]))
    val inputStream = ConcatenatingJarInputStream(inputStreams)
    inputStream.copyTo(Paths.get(outputFilename), StandardCopyOption.REPLACE_EXISTING)
}