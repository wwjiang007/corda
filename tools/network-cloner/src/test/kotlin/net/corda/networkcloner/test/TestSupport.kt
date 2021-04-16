package net.corda.networkcloner.test

import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.toPath
import net.corda.networkcloner.api.IdentityRepository
import net.corda.networkcloner.api.Serializer
import net.corda.networkcloner.impl.IdentityRepositoryImpl
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
            val pathToCordapps = SerializerTests::class.java.getResource("/snapshots/$snapshot/source/cordapps").path
            SerializerImpl(Paths.get(pathToCordapps)).also {
                serializer = it
            }
        } else {
            serializer!!
        }
    }

    fun getIdentityMapper(snapshot : String) : IdentityRepository {
        val testRootDir = TestSupport::class.java.getResource("/snapshots/$snapshot").toPath().toFile()

        val sourceCertificatesDirs = listOf("client","operator").map { File(testRootDir, "source/$it/certificates") }
        val destinationCertificatesDirs = listOf("client","operator").map { File(testRootDir, "destination/$it/certificates") }

        (sourceCertificatesDirs + destinationCertificatesDirs).forEach {
            assertTrue(it.exists(), "Path '$it' must exist.")
            assertTrue(it.isDirectory(), "Path '$it' must be a directory.")
        }

        return IdentityRepositoryImpl(sourceCertificatesDirs, destinationCertificatesDirs)
    }

    companion object {
        var serializer : Serializer? = null
    }

}