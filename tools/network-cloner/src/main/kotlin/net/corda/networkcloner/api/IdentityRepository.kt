package net.corda.networkcloner.api

import net.corda.core.identity.CordaX500Name
import net.corda.networkcloner.entity.Identity
import java.security.PublicKey

interface IdentityRepository {

    fun getAllIdentities() : List<Identity>
    fun getIdentityBySourcePublicKey(publicKey: PublicKey) : Identity?
    fun getIdentityBySourceX500Name(x500Name : CordaX500Name) : Identity?

}