package net.corda.core.internal

import net.corda.core.utilities.loggerFor
import net.corda.core.utilities.trace
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import kotlin.collections.HashSet

/*
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
 */
/*
class NestedJarInputStream(inputStream: InputStream, name: String) : PipedInputStream(64 * 1024) {
    private val outputStream = PipedOutputStream()
    private val entries = HashSet<String>()

    init {
        connect(outputStream)
        thread(name = "$this-Thread", isDaemon = true) {
            var jarOutputStream: JarOutputStream? = null
            try {
                jarOutputStream = copyInputStream(jarOutputStream, NamedInputStream(inputStream, name))
                jarOutputStream?.close()
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                inputStream.close()
                outputStream.close()
            }
        }
    }

    private fun copyInputStream(jarOutputStream: JarOutputStream?, inputStream: NamedInputStream): JarOutputStream? {
        var jarOutputStream1 = jarOutputStream
        println("Processing input stream ${inputStream.name}")
        val jarInputStream = JarInputStream(inputStream)
        if (jarOutputStream1 == null) {
            val manifest = jarInputStream.manifest
            if (manifest == null) {
                println("No manifest")
                jarOutputStream1 = JarOutputStream(outputStream)
            } else {
                println("Manifest $manifest")
                jarOutputStream1 = JarOutputStream(outputStream, manifest)
            }
        }
        copyJar(jarOutputStream1, jarInputStream, inputStream.name)
        return jarOutputStream1
    }

    private fun copyJar(jarOutputStream: JarOutputStream, jarInputStream: JarInputStream, jarName: String) {
        var jarEntry = jarInputStream.nextJarEntry
        while (jarEntry != null) {
            if (jarEntry.name.endsWith(".jar")) {
                copyInputStream(jarOutputStream, NamedInputStream(jarInputStream, "$jarName -> ${jarEntry.name}"))
            } else if (entries.add(jarEntry.name)) {
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
*/

open class SingleThreadPipedInputStream(bufferSize: Int) : InputStream() {
    protected open val logger = loggerFor<SingleThreadPipedInputStream>()
    private var buffer = ByteArray(bufferSize)
    private var pos = 0
    private var limit = 0

    override fun read(): Int {
        return if (pos < limit) {
            buffer[pos++].toInt() and 0xff
        } else -1
    }

    override fun available(): Int {
        val remaining = limit - pos
        if (remaining == 0) {
            logger.trace { "Reset" }
            pos = 0
            limit = 0
        }
        return remaining
    }

    override fun close() {
        buffer = ByteArray(0)
        pos = 0
        limit = 0
    }

    protected val outputStream = object : OutputStream() {
        override fun write(b: Int) {
            if (limit == buffer.size) {
                if (pos == 0) {
                    if (limit == 0) throw IOException("Input stream closed")
                    val newBuffer = ByteArray(limit * 2)
                    logger.trace { "Resize to ${newBuffer.size}" }
                    System.arraycopy(buffer, 0, newBuffer, 0, limit)
                    buffer = newBuffer
                } else {
                    val remaining = limit - pos
                    logger.trace { "Shuffle $remaining" }
                    System.arraycopy(buffer, pos, buffer, 0, remaining)
                    limit = remaining
                    pos = 0
                }
            }
            buffer[limit++] = b.toByte()
        }
    }
}

class NestedJarInputStream(inputStream: InputStream, name: String) : SingleThreadPipedInputStream(64 * 1024) {
    override val logger = loggerFor<NestedJarInputStream>()
    private var jarOutputStream: JarOutputStream? = null
    private val inputStreams = Stack<Pair<String, JarInputStream>>()
    private val entries = HashSet<String>()

    init {
        pushInputStream(inputStream, name)
    }

    override fun read(): Int {
        topUp()
        return super.read()
    }

    private fun topUp() {
        while (available() == 0 && !inputStreams.empty()) {
            val (name, jarInputStream) = inputStreams.peek()
            nextEntry(jarOutputStream!!, jarInputStream, name)
        }
    }

    private fun pushInputStream(inputStream: InputStream, name: String) {
        var jarOutputStream1 = jarOutputStream
        logger.trace { "Processing input stream $name" }
        val jarInputStream = JarInputStream(inputStream)
        inputStreams.push(name to jarInputStream)
        if (jarOutputStream1 == null) {
            val manifest = jarInputStream.manifest
            if (manifest == null) {
                logger.trace { "No manifest" }
                jarOutputStream1 = JarOutputStream(outputStream)
            } else {
                logger.trace { "Manifest $manifest" }
                jarOutputStream1 = JarOutputStream(outputStream, manifest)
            }
            jarOutputStream = jarOutputStream1
        }
    }

    private fun nextEntry(jarOutputStream: JarOutputStream, jarInputStream: JarInputStream, jarName: String) {
        var jarEntry = jarInputStream.nextJarEntry
        if (jarEntry != null) {
            if (jarEntry.name.endsWith(".jar")) {
                pushInputStream(jarInputStream, "$jarName -> ${jarEntry.name}")
            } else {
                if (entries.add(jarEntry.name)) {
                    if (!skipOrSubstitute(jarOutputStream, jarEntry, jarName)) {
                        val size = jarEntry.size
                        copyEntry(jarOutputStream, jarEntry, jarInputStream, jarName, size)
                        jarOutputStream.flush()
                    }
                } else if (!jarEntry.isDirectory) {
                    logger.trace { "Skipping duplicate entry ${jarEntry.name} in $jarName" }
                }
                jarInputStream.closeEntry()
            }
        } else {
            if (!inputStreams.empty()) {
                inputStreams.pop()
                logger.trace { "Finished $jarName" }
                if (!inputStreams.empty()) {
                    val (_, peekedJarInputStream) = inputStreams.peek()
                    peekedJarInputStream.closeEntry()
                } else {
                    logger.trace { "Closed" }
                    jarInputStream.close()
                    jarOutputStream.close()
                }
            }
        }
    }

    protected fun skipOrSubstitute(jarOutputStream: JarOutputStream, jarEntry: JarEntry, jarName: String): Boolean {
        return false
    }

    private fun copyEntry(jarOutputStream: JarOutputStream, jarEntry: JarEntry, jarInputStream: JarInputStream, jarName: String, size: Long) {
        logger.trace { "Copy entry ${jarEntry.name} from $jarName of size $size" }
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
    //val inputStreams = JarOfJarsIterator(FileInputStream(args[1]))
    //val inputStream = ConcatenatingJarInputStream(inputStreams)
    val inputStream = NestedJarInputStream(FileInputStream(args[1]), args[1])
    inputStream.copyTo(Paths.get(outputFilename), StandardCopyOption.REPLACE_EXISTING)
}