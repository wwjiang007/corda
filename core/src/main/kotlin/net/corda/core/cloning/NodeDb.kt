package net.corda.core.cloning

interface NodeDb {

    fun <T> readEntities() : List<T>
    fun <T> writeEntities(entities : List<T>)

}