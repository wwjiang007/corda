package net.corda.networkcloner.api

interface TransactionsStore {

    fun getAllTransactions(url: String, username: String, password: String) : List<ByteArray>

}