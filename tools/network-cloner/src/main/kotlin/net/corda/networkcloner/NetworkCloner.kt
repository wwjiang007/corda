package net.corda.networkcloner

import net.corda.core.internal.createComponentGroups
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.networkcloner.impl.CordappsRepositoryImpl
import net.corda.networkcloner.impl.SerializerImpl
import net.corda.networkcloner.impl.SignerImpl
import net.corda.networkcloner.impl.NodeDatabaseImpl
import net.corda.networkcloner.impl.NodesToNodesMigrationTaskFactory
import net.corda.networkcloner.runnable.DefaultMigration
import net.corda.serialization.internal.CordaSerializationEncoding
import java.io.File
import java.nio.file.Paths

fun main(args: Array<String>) {
    val sourceDir = File(args[0])
    val destDir = File(args[1])
    val pathToCordapps = File(args[2])

    require(sourceDir.exists() && sourceDir.isDirectory) {"$sourceDir must exist and be a directory"}
    require(destDir.exists() && destDir.isDirectory) {"$destDir must exist and be a directory"}
    require(pathToCordapps.exists() && pathToCordapps.isDirectory) {"$pathToCordapps must exist and be a directory"}

    println("The migration is commencing from $sourceDir to $destDir")

    val migrationTaskFactory = NodesToNodesMigrationTaskFactory(sourceDir, destDir)
    val migrationTasks = migrationTaskFactory.getMigrationTasks()
    println("There are ${migrationTasks.size} migration tasks: $migrationTasks")

    val cordappsRepository = CordappsRepositoryImpl(pathToCordapps, 1)
    val serializer = SerializerImpl(cordappsRepository.getCordappLoader())
    val signer = SignerImpl()
    migrationTasks.filterNot { it.identity.sourceParty.toString().contains("notary", true) }.forEach {
        DefaultMigration(it, serializer, signer, cordappsRepository, true).run()
    }
}