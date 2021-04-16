package net.corda.networkcloner.impl

import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.networkcloner.api.IdentityRepository
import net.corda.networkcloner.entity.Identity
import net.corda.networkcloner.entity.PartyAndPrivateKey
import net.corda.nodeapi.internal.crypto.getCertificateAndKeyPair
import net.corda.nodeapi.internal.crypto.x509
import java.io.File
import java.io.FileInputStream
import java.lang.RuntimeException
import java.security.KeyStore

class IdentityRepositoryImpl(sourceCertificatesDirs : List<File>, destinationCertificatesDirs : List<File>) : IdentityRepository {

    val identities : List<Identity>

    init {
        val sourceParties = sourceCertificatesDirs.map { loadSourceParty(it) }
        val destinationPartiesAndPrivateKeys = destinationCertificatesDirs.map { loadDestinationPartyAndPrivateKey(it) }
        if (sourceParties.size != destinationPartiesAndPrivateKeys.size) {
            throw RuntimeException("There must be equal number of source and destination parties")
        }
        identities = sourceParties.map { sourceParty ->
            val destinationPartyAndPrivateKey = destinationPartiesAndPrivateKeys.find { it.party.name == sourceParty.name } ?: throw RuntimeException("Source party ${sourceParty.name} doesn't have a matching destination party")
            Identity(sourceParty, destinationPartyAndPrivateKey)
        }
    }

    private fun loadSourceParty(certificatesDirectory : File) : Party {
        val ks  = KeyStore.getInstance("JKS")
        ks.load(FileInputStream(File(certificatesDirectory,"nodekeystore.jks")),"cordacadevpass".toCharArray())
        val certificate = ks.getCertificate("identity-private-key").x509
        return Party(certificate)
    }

    private fun loadDestinationPartyAndPrivateKey(certificatesDirectory : File) : PartyAndPrivateKey {
        val ks  = KeyStore.getInstance("JKS")
        ks.load(FileInputStream(File(certificatesDirectory,"nodekeystore.jks")),"cordacadevpass".toCharArray())
        val identityKey = ks.getCertificateAndKeyPair("identity-private-key", "cordacadevpass")
        return PartyAndPrivateKey(Party(identityKey.certificate), identityKey.keyPair.private)
    }

    override fun getIdentityBySourceX500Name(x500Name: CordaX500Name): Identity? {
        return identities.find { it.sourceParty.name == x500Name }
    }
}