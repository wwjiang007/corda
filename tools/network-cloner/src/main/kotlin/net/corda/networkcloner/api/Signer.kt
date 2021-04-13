package net.corda.networkcloner.api

import net.corda.core.crypto.TransactionSignature
import net.corda.core.transactions.CoreTransaction
import java.security.PublicKey

interface Signer {

    fun sign(transaction : CoreTransaction, originalSigners : List<PublicKey>) : List<TransactionSignature>

}