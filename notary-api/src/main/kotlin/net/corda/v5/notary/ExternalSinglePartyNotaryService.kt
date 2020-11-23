package net.corda.v5.notary

import net.corda.core.internal.notary.SinglePartyNotaryService

// FIXME: This should replace SinglePartyNotaryService, replacing constructor arguments with
// NotaryServiceProperties. We derive for the time being until we can adapt the built in
// notary to the new interface / remove the redundant notary implementations.
abstract class ExternalSinglePartyNotaryService : SinglePartyNotaryService() {
    abstract val notaryServiceProperties: NotaryServiceProperties
}