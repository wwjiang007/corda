package net.corda.networkcloner.impl

import net.corda.networkcloner.api.MigrationTaskFactory
import java.io.File

class NodesToNodesMigrationTaskFactory(source : File, destination : File) : MigrationTaskFactory {

    override fun getMigrationTasks() {
        //todo
    }
}