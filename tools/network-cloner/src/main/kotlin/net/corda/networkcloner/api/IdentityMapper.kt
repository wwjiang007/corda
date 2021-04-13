package net.corda.networkcloner.api

import net.corda.networkcloner.entity.Identity
import java.security.PublicKey

interface IdentityMapper {

    fun mapPublicKeyToDestinationIdentity(sourcePublicKey : PublicKey) : Identity

}