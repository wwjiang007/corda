package net.corda.networkcloner.api

import net.corda.core.identity.CordaX500Name
import net.corda.networkcloner.entity.Identity

interface IdentityRepository {

    fun getIdentityBySourceX500Name(x500Name : CordaX500Name) : Identity?

}