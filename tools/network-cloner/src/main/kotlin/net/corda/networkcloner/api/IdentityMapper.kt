package net.corda.networkcloner.api

import net.corda.core.identity.CordaX500Name
import net.corda.networkcloner.entity.Identity
import java.security.PublicKey

interface IdentityMapper {

    fun getSourceIdentity(x500 : CordaX500Name)
    fun getDestinationIdentity(x500: CordaX500Name)
    fun mapPublicKeyToDestinationIdentity(sourcePublicKey : PublicKey) : Identity

}