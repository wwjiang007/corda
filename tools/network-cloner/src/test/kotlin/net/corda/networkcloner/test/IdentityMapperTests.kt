package net.corda.networkcloner.test

import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.toPath
import net.corda.networkcloner.impl.IdentityMapperImpl
import org.junit.Test
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IdentityMapperTests : TestSupport() {

    @Test
    fun `JKS files can be read and parsed into identities`() {
        val identityMapper = getIdentityMapper("s1")

        assertNotNull(identityMapper.getSourceIdentity(CordaX500Name.parse("O=Operator,L=New York,C=US")))
        assertNotNull(identityMapper.getSourceIdentity(CordaX500Name.parse("O=Client,L=London,C=GB")))

        assertNotNull(identityMapper.getDestinationIdentity(CordaX500Name.parse("O=Operator,L=New York,C=US")))
        assertNotNull(identityMapper.getDestinationIdentity(CordaX500Name.parse("O=Client,L=London,C=GB")))
    }

}