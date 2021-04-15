package net.corda.networkcloner.impl

import net.corda.core.identity.CordaX500Name
import net.corda.networkcloner.api.IdentityMapper
import net.corda.networkcloner.entity.Identity
import net.corda.nodeapi.internal.crypto.getCertificateAndKeyPair
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PublicKey

class IdentityMapperImpl(sourceCertificatesDirs : List<File>, destinationCertificatesDirs : List<File>) : IdentityMapper {

    val sourceIdentities = sourceCertificatesDirs.flatMap { loadIdentities(it) }
    val destinationIdentities = destinationCertificatesDirs.flatMap { loadIdentities(it) }

    override fun getSourceIdentity(x500Name: CordaX500Name) : Identity? {
        return sourceIdentities.find { it.x500Name == x500Name }
    }

    override fun getDestinationIdentity(x500Name: CordaX500Name) : Identity? {
        return destinationIdentities.find { it.x500Name == x500Name }
    }

    private fun loadIdentities(certificatesDirectory : File) : List<Identity> {
        val ks  = KeyStore.getInstance("JKS")
        ks.load(FileInputStream(File(certificatesDirectory,"nodekeystore.jks")),"cordacadevpass".toCharArray())
        val identityKey = ks.getCertificateAndKeyPair("identity-private-key", "cordacadevpass")
        val cordaX500Name = CordaX500Name.build(identityKey.certificate.subjectX500Principal)
        return listOf(Identity(cordaX500Name, identityKey.keyPair))
    }
}