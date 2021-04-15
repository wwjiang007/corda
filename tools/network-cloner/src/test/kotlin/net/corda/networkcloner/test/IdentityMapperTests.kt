package net.corda.networkcloner.test

import net.corda.core.internal.toPath
import net.corda.networkcloner.impl.IdentityMapperImpl
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class IdentityMapperTests {

    @Test
    fun `JKS files can be read and parsed into identities`() {
        val testRootDir = IdentityMapperTests::class.java.getResource("/snapshots/s1").toPath().toFile()

        val sourceCertificatesDirs = listOf("client","operator").map { File(testRootDir, "source/$it/certificates") }
        val destinationCertificatesDirs = listOf("client","operator").map { File(testRootDir, "destination/$it/certificates") }

        (sourceCertificatesDirs + destinationCertificatesDirs).forEach {
            assertTrue(it.exists(), "Path '$it' must exist.")
            assertTrue(it.isDirectory(), "Path '$it' must be a directory.")
        }

        val identityMapper = IdentityMapperImpl(sourceCertificatesDirs, destinationCertificatesDirs)
        assertTrue(identityMapper.sourceIdentities.isNotEmpty())
        assertTrue(identityMapper.destinationIdentities.isNotEmpty())
    }

}