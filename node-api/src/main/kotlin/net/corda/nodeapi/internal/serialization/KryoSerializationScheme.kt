package net.corda.nodeapi.internal.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.SerializedBytes
import net.corda.core.utilities.ByteSequence
import net.corda.serialization.internal.CordaSerializationMagic
import net.corda.serialization.internal.SerializationScheme
import net.corda.serialization.internal.amqp.amqpMagic
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class KryoSerializationScheme: SerializationScheme {
    companion object {
        const val KRYO_MAGIC_BYTE = 0xAA.toByte()
    }
    val kryoMagic = CordaSerializationMagic(byteArrayOf(KRYO_MAGIC_BYTE).take(amqpMagic.size).toByteArray())

    override fun canDeserializeVersion(magic: CordaSerializationMagic, target: SerializationContext.UseCase): Boolean {
        return magic == kryoMagic && target == SerializationContext.UseCase.P2P
    }

    override fun <T : Any> deserialize(byteSequence: ByteSequence, clazz: Class<T>, context: SerializationContext): T {
        val kryo = Kryo()
        kryo.classLoader = context.deserializationClassLoader
        val obj = Input(ByteArrayInputStream(byteSequence.bytes)).use {
            kryo.readClassAndObject(it)
        }
        return obj as T
    }

    override fun <T : Any> serialize(obj: T, context: SerializationContext): SerializedBytes<T> {
        val kryo = Kryo()
        kryo.classLoader = context.deserializationClassLoader
        val outputStream = ByteArrayOutputStream()
        Output(outputStream).use {
            kryo.writeClassAndObject(it, obj)
        }
        return SerializedBytes(outputStream.toByteArray())
    }
}