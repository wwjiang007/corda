package net.corda.networkcloner.api

import net.corda.core.transactions.SignedTransaction

interface Serializer {

    fun serializeSignedTransaction(signedTransaction: SignedTransaction): ByteArray
    fun deserializeDbBlobIntoTransaction(byteArray: ByteArray) : SignedTransaction

}