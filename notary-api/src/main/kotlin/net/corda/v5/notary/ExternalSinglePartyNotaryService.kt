package net.corda.v5.notary

import net.corda.core.internal.notary.SinglePartyNotaryService

abstract class ExternalSinglePartyNotaryService : SinglePartyNotaryService() {
    abstract val notaryServiceProperties: NotaryServiceProperties
}