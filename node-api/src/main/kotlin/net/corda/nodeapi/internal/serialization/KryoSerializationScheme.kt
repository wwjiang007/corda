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
import org.objenesis.instantiator.ObjectInstantiator
import org.objenesis.strategy.InstantiatorStrategy
import org.objenesis.strategy.StdInstantiatorStrategy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.reflect.Modifier

class KryoSerializationScheme: SerializationScheme {
    companion object {
        const val KRYO_MAGIC_BYTE = 0xAA.toByte()
        val kryoMagic = CordaSerializationMagic(ByteArray(amqpMagic.size) {KRYO_MAGIC_BYTE} )
    }

    override fun canDeserializeVersion(magic: CordaSerializationMagic, target: SerializationContext.UseCase): Boolean {
        return magic == kryoMagic && target == SerializationContext.UseCase.P2P
    }

    override fun <T : Any> deserialize(byteSequence: ByteSequence, clazz: Class<T>, context: SerializationContext): T {
        val kryo = Kryo()
        kryo.instantiatorStrategy = CustomInstantiatorStrategy()
        kryo.classLoader = context.deserializationClassLoader
        val obj = Input(ByteArrayInputStream(byteSequence.bytes, amqpMagic.size, byteSequence.bytes.size)).use {
            kryo.readClassAndObject(it)
        }
        return obj as T
    }

    override fun <T : Any> serialize(obj: T, context: SerializationContext): SerializedBytes<T> {
        val kryo = Kryo()
        kryo.instantiatorStrategy = CustomInstantiatorStrategy()
        kryo.classLoader = context.deserializationClassLoader
        val outputStream = ByteArrayOutputStream()
        outputStream.write(kryoMagic.bytes)
        Output(outputStream).use {
            kryo.writeClassAndObject(it, obj)
        }
        return SerializedBytes(outputStream.toByteArray())
    }

    //Stolen from DefaultKryoCustomizer.kt
    private class CustomInstantiatorStrategy : InstantiatorStrategy {
        private val fallbackStrategy = StdInstantiatorStrategy()
        // Use this to allow construction of objects using a JVM backdoor that skips invoking the constructors, if there
        // is no no-arg constructor available.
        private val defaultStrategy = Kryo.DefaultInstantiatorStrategy(fallbackStrategy)

        override fun <T> newInstantiatorOf(type: Class<T>): ObjectInstantiator<T> {
            // However this doesn't work for non-public classes in the java. namespace
            val strat = if (type.name.startsWith("java.") && !Modifier.isPublic(type.modifiers)) fallbackStrategy else defaultStrategy
            return strat.newInstantiatorOf(type)
        }
    }
    //End stolen code
}