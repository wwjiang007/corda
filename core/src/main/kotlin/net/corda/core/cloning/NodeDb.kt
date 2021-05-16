package net.corda.core.cloning

interface NodeDb {

    fun <T> readEntities(clazz: Class<T>) : List<T>
    fun <T> writeEntities(entities : List<T>)

}