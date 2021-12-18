package net.corda.node.services.telemetry

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.context.Scope
import net.corda.core.node.services.SerializableSpanContext
import net.corda.core.node.services.TelemetryService
import net.corda.core.serialization.SingletonSerializeAsToken
import java.util.*
import java.util.concurrent.ConcurrentHashMap

//@todo
//1. auto instrumentation to reveal db
//2. signing                                                         TICK
//3. rpc client
//4. try on enterprise
//5. allow disabling it
//6. take configuration from config file
//7. the span should start only after it was added to the map        TICK
//8. dev mode false

data class SpanInfo(var span : Span?, var scope: Scope?, var spanBuilder: SpanBuilder?) {
    fun buildAndStartSpan() {
        if (span != null) {
            throw IllegalStateException("Expected span to be null")
        }
        span = requireSpanBuilder().startSpan()
        scope = requireSpan().makeCurrent()
        spanBuilder = null
    }

    fun requireSpanBuilder() = spanBuilder ?: throw IllegalStateException("Expected span builder not to be null.")
    fun requireSpan() = span ?: throw IllegalStateException("Expected span not to be null.")
    fun requireScope() = scope ?: throw IllegalStateException("Expected scope not to be null.")

    fun endSpanAndCloseScope() {
        requireSpan().end()
        requireScope().close()
    }
}

class OpenTelemetryService : SingletonSerializeAsToken(), TelemetryService {

    private val spans = ConcurrentHashMap<UUID, SpanInfo>()

    override fun startSpan(name: String, attributes : Map<String,String>) : UUID {
        val attributesMap = attributes.toList().fold(Attributes.builder()) { builder, attribute -> builder.put(attribute.first, attribute.second) }.build()
        val tracer = GlobalOpenTelemetry.getTracerProvider().get(OpenTelemetryService::class.java.name)
        val spanBuilder = tracer.spanBuilder(name).setAllAttributes(attributesMap)
        return addSpanAndStartIt(spanBuilder)
    }

    override fun endSpan(spanId : UUID) {
        spans.remove(spanId)?.endSpanAndCloseScope()
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

    private fun addSpanAndStartIt(spanBuilder: SpanBuilder) : UUID {
        val spanId = UUID.randomUUID()
        val spanInfo = SpanInfo(null, null, spanBuilder) //it's done this way so we only start the span when it has been added to the map (so that we don't count that to the span latency)
        spans[spanId] = spanInfo
        spanInfo.buildAndStartSpan()
        return spanId
    }

    private fun addSpan(span : Span) : UUID {
        val spanId = UUID.randomUUID()
        spans[spanId] = SpanInfo(span, span.makeCurrent(), null)
        return spanId
    }
}