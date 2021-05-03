package net.corda.networkcloner.impl

import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.crypto.TransactionSignature
import net.corda.core.crypto.sign
import net.corda.core.transactions.CoreTransaction
import net.corda.networkcloner.api.Signer
import java.security.KeyPair

class SignerImpl : Signer {

    override fun sign(transactionId: SecureHash, signers: List<KeyPair>): List<TransactionSignature> {
        val signableData = SignableData(transactionId, SignatureMetadata(9, 4))
        return signers.map {
            it.sign(signableData)
        }
    }

}