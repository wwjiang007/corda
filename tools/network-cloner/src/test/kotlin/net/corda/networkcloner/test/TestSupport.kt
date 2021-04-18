package net.corda.networkcloner.test

import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.toPath
import net.corda.networkcloner.api.Serializer
import net.corda.networkcloner.impl.SerializerImpl
import java.io.File
import java.nio.file.Paths
import kotlin.test.assertTrue

open class TestSupport {

    val clientX500Name = CordaX500Name.parse("O=Client,L=London,C=GB")
    val operatorX500Name = CordaX500Name.parse("O=Operator,L=New York,C=US")

    //@todo this storing to a static property doesn't really work if different tests ask for different snapshot
    fun getSerializer(snapshot: String) : Serializer {
        return if (serializer == null) {
            val pathToCordapps = SerializerTests::class.java.getResource("/snapshots/$snapshot/source/client/cordapps").path
            SerializerImpl(Paths.get(pathToCordapps)).also {
                serializer = it
            }
        } else {
            serializer!!
        }
    }

    fun getSnapshotDirectory(snapshot: String) : File {
        return TestSupport::class.java.getResource("/snapshots/$snapshot").toPath().toFile()
    }

    companion object {
        var serializer : Serializer? = null
    }

}