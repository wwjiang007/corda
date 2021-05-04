package net.corda.networkcloner.util

import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.node.services.persistence.AbstractPartyToX500NameAsStringConverter
import org.hibernate.boot.MetadataBuilder
import org.hibernate.boot.spi.MetadataBuilderContributor

class AttributeConverterMetadataBuilderContributor(private val wellKnownPartyFromX500Name: (CordaX500Name) -> Party?, private val wellKnownPartyFromAnonymous: (AbstractParty) -> Party?) : MetadataBuilderContributor {

    override fun contribute(metadataBuilder: MetadataBuilder?) {

        metadataBuilder?.applyAttributeConverter(AbstractPartyToX500NameAsStringConverter(wellKnownPartyFromX500Name, wellKnownPartyFromAnonymous))
    }
}