package net.corda.networkcloner.entity

import net.corda.core.cloning.Identity

data class MigrationContext(val identities : List<Identity>)
