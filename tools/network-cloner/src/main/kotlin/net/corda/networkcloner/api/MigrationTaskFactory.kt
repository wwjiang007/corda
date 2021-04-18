package net.corda.networkcloner.api

import net.corda.networkcloner.entity.MigrationTask

interface MigrationTaskFactory {

    fun getMigrationTasks() : List<MigrationTask>

}