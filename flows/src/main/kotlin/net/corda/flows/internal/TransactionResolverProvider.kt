package net.corda.flows.internal

import net.corda.core.internal.TransactionsResolver

interface TransactionResolverProvider {
    fun createTransactionsResolver(flow: ResolveTransactionsFlow): TransactionsResolver
}