package net.corda.networkcloner.api

interface TransactionsStore {

    fun getAllTransactions() : List<ByteArray>

}