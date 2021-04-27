package net.corda.networkcloner.runnable

import net.corda.networkcloner.entity.MigrationTask

class Migration(val migrationTask: MigrationTask) : Runnable {

    override fun run() {
        val migrationData = migrationTask.sourceNodeDatabase.readMigrationData()
    }
}