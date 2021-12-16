package net.corda.node.services.telemetry

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Scope
import net.corda.core.node.services.SerializableSpanContext
import net.corda.core.node.services.TelemetryService
import net.corda.core.serialization.SingletonSerializeAsToken
import java.util.*
import java.util.concurrent.ConcurrentHashMap

//@todo
//1. auto instrumentation to reveal db
//2. signing
//3. rpc client
//4. try on enterprise
//5. allow disabling it
//6. take configuration from config file
//7. the span should start only after it was added to the map
//8. dev mode false

data class SpanInfo(val span : Span, val scope: Scope)

class OpenTelemetryService(serviceName : String) : SingletonSerializeAsToken(), TelemetryService {

    private val spans = ConcurrentHashMap<UUID, SpanInfo>()

    override fun startSpan(name: String, attributes : Map<String,String>) : UUID {
        val attributesMap = attributes.toList().fold(Attributes.builder()) { builder, attribute -> builder.put(attribute.first, attribute.second) }.build()
        val tracer = GlobalOpenTelemetry.getTracerProvider().get(OpenTelemetryService::class.java.name)
        val spanBuilder = tracer.spanBuilder(name).setAllAttributes(attributesMap)
        val span = spanBuilder.startSpan()
        return addSpan(span)
    }

    override fun endSpan(spanId : UUID) {
        spans.remove(spanId)?.let {
            it.span.end()
            it.scope.close()
        }
    }

    override fun getCurrentSpanContext(): SerializableSpanContext {
        return SerializableSpanContext(Span.current().spanContext)
    }

    override fun getSpanContext(spanId: UUID): SerializableSpanContext {
        val spanContext = spans[spanId]?.span?.spanContext ?: throw IllegalArgumentException("Couldn't find a span for id ${spanId}")
        return SerializableSpanContext(spanContext)
    }

    override fun addRemoteSpan(serializableSpanContext: SerializableSpanContext): UUID {
        val spanContext = serializableSpanContext.createRemoteSpanContext()
        val span = Span.wrap(spanContext)
        return addSpan(span)
    }

    override fun addRemoteSpanAndStartChildSpan(serializableSpanContext: SerializableSpanContext, name: String, attributes: Map<String, String>): UUID {
        addRemoteSpan(serializableSpanContext)
        return startSpan(name, attributes)
    }

    private fun addSpan(span : Span) : UUID {
        val spanId = UUID.randomUUID()
        spans[spanId] = SpanInfo(span, span.makeCurrent())
        return spanId
    }
}