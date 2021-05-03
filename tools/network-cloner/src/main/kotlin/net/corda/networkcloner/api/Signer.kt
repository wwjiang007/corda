package net.corda.networkcloner.api

import net.corda.core.cloning.PartyAndPrivateKey
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.TransactionSignature
import net.corda.core.transactions.CoreTransaction
import java.security.KeyPair
import java.security.PublicKey

interface Signer {

    fun sign(transactionId: SecureHash, signers : List<KeyPair>) : List<TransactionSignature>

}