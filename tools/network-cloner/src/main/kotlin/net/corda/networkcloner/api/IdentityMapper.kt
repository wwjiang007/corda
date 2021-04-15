package net.corda.networkcloner.api

import net.corda.core.identity.CordaX500Name
import net.corda.networkcloner.entity.Identity
import java.security.PublicKey

interface IdentityMapper {

    fun getSourceIdentity(x500Name : CordaX500Name) : Identity?
    fun getDestinationIdentity(x500Name: CordaX500Name) : Identity?

}