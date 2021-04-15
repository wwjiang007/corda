package net.corda.networkcloner.entity

import net.corda.core.identity.CordaX500Name
import java.security.KeyPair

data class Identity(val x500Name: CordaX500Name, val identityKey : KeyPair)
