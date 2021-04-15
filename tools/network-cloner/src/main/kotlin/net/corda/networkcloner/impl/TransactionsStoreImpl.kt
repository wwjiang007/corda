package net.corda.networkcloner.impl

import net.corda.networkcloner.api.TransactionsStore
import java.sql.DriverManager

class TransactionsStoreImpl(val url : String, val username: String, val password: String) : TransactionsStore {

    override fun getAllTransactions(): List<ByteArray> {
        val driver = Class.forName("org.h2.Driver")
        val con = DriverManager.getConnection(url, username, password)

        val statement = con.createStatement()
        val resultSet = statement.executeQuery("SELECT TRANSACTION_VALUE FROM NODE_TRANSACTIONS")


        val ret = mutableListOf<ByteArray>()
        while (resultSet.next()) {
            ret.add(resultSet.getBytes(1))
        }

        con.close()
        
        return ret
    }
}