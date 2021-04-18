package net.corda.networkcloner.impl

import net.corda.networkcloner.api.MigrationTaskFactory
import net.corda.networkcloner.entity.MigrationTask
import java.io.File

class NodesToNodesMigrationTaskFactory(val source : File, val destination : File) : MigrationTaskFactory {

    override fun getMigrationTasks() : List<MigrationTask> {
        val sourceParties = NodesDirectoryPartyRepository(source)
        val destinationParties = NodesDirectoryPartyRepository(destination)
        return emptyList()
    }
}