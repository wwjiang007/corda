package net.corda.networkcloner.impl

import net.corda.core.identity.Party
import net.corda.networkcloner.api.PartyRepository
import net.corda.networkcloner.entity.PartyAndPrivateKey
import net.corda.nodeapi.internal.crypto.getCertificateAndKeyPair
import net.corda.nodeapi.internal.crypto.x509
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore

class NodesDirectoryPartyRepository(nodesDirectory: File) : PartyRepository {

    val certificatesDirectories = getCertificatesDirectories(nodesDirectory)

    private fun getCertificatesDirectories(nodesDirectory: File): List<File> {
        return nodesDirectory.listFiles().filter { it.isDirectory }.mapNotNull {
            val certificatesDir = File(it, "certificates")
            if (certificatesDir.exists() && certificatesDir.isDirectory) {
                certificatesDir
            } else {
                null
            }
        }.also {
            println("Located these certificates directories: $it")
        }
    }

    override fun getParties(): List<Party> {
        return certificatesDirectories.map {
            val ks = KeyStore.getInstance("JKS")
            ks.load(FileInputStream(File(it, "nodekeystore.jks")), "cordacadevpass".toCharArray())
            val certificate = ks.getCertificate("identity-private-key").x509
            Party(certificate)
        }
    }

    override fun getPartiesWithPrivateKeys(): List<PartyAndPrivateKey> {
        return certificatesDirectories.map {
            val ks  = KeyStore.getInstance("JKS")
            ks.load(FileInputStream(File(it,"nodekeystore.jks")),"cordacadevpass".toCharArray())
            val identityKey = ks.getCertificateAndKeyPair("identity-private-key", "cordacadevpass")
            PartyAndPrivateKey(Party(identityKey.certificate), identityKey.keyPair.private)
        }
    }
}