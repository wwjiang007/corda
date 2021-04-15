package net.corda.networkcloner.test

import net.corda.core.internal.toPath
import net.corda.networkcloner.api.IdentityMapper
import net.corda.networkcloner.impl.IdentityMapperImpl
import java.io.File
import kotlin.test.assertTrue

open class TestSupport {

    fun getIdentityMapper(snapshot : String) : IdentityMapper {
        val testRootDir = TestSupport::class.java.getResource("/snapshots/$snapshot").toPath().toFile()

        val sourceCertificatesDirs = listOf("client","operator").map { File(testRootDir, "source/$it/certificates") }
        val destinationCertificatesDirs = listOf("client","operator").map { File(testRootDir, "destination/$it/certificates") }

        (sourceCertificatesDirs + destinationCertificatesDirs).forEach {
            assertTrue(it.exists(), "Path '$it' must exist.")
            assertTrue(it.isDirectory(), "Path '$it' must be a directory.")
        }

        return IdentityMapperImpl(sourceCertificatesDirs, destinationCertificatesDirs)
    }

}