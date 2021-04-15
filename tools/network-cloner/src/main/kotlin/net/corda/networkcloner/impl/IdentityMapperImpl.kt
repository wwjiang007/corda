package net.corda.networkcloner.impl

import net.corda.core.identity.CordaX500Name
import net.corda.networkcloner.api.IdentityMapper
import net.corda.networkcloner.entity.Identity
import net.corda.nodeapi.internal.crypto.getCertificateAndKeyPair
import net.corda.nodeapi.internal.crypto.getSupportedKey
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyStore
import java.security.PublicKey

class IdentityMapperImpl(sourceCertificates : List<Path>, destinationCertificates : List<Path>) : IdentityMapper {


    val destinationIdentities = loadDestinationIdentities()

    override fun getSourceIdentity(x500: CordaX500Name) {
        TODO("Not yet implemented")
    }

    override fun getDestinationIdentity(x500: CordaX500Name) {
        TODO("Not yet implemented")
    }

    override fun mapPublicKeyToDestinationIdentity(sourcePublicKey: PublicKey) : Identity {
        return destinationIdentities.single()
    }

    private fun loadIdentities(certificates : Path) : List<Identity> {
        val ks  = KeyStore.getInstance("JKS")
        return emptyList()
    }

    private fun loadDestinationIdentities() : List<Identity> {
        val ks  = KeyStore.getInstance("JKS")
        ks.load(FileInputStream(File("/Users/alex.koller/Projects/contract-sdk/examples/test-app/buildDestination/nodes/Operator/certificates/nodekeystore.jks")),"cordacadevpass".toCharArray())

        val x = ks.getCertificateAndKeyPair("identity-private-key", "cordacadevpass")

        return listOf(Identity(x.keyPair))
    }
}