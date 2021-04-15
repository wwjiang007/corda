package net.corda.networkcloner.test

import net.corda.networkcloner.impl.TransactionsStoreImpl
import org.junit.Test

class SerializerTests {

    @Test
    fun `Transaction blob is identical when gone through deserialization and serialization`() {
        val pathToTestDb = SerializerTests::class.java.getResource("/database/persistence.mv.db").path.removeSuffix(".mv.db")
        val transactionsStore = TransactionsStoreImpl("jdbc:h2:$pathToTestDb","sa","")
        val dbTx = transactionsStore.getAllTransactions().first()

        println(dbTx)
    }

}