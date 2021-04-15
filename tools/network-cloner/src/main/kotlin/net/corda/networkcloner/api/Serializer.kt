package net.corda.networkcloner.api

import net.corda.core.transactions.SignedTransaction

interface Serializer {

    fun deserializeDbBlobIntoTransaction(byteArray: ByteArray) : SignedTransaction

}