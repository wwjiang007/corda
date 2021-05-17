package net.corda.networkcloner.entity

data class MigrationData(val coreCordaData: CoreCordaData, val entities : Map<Class<out Any>,List<Any>>)
