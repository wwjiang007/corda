package net.corda.core.node.services

import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import net.corda.core.serialization.CordaSerializable
import java.util.*


@CordaSerializable
data class SerializableSpanContext(val traceIdHex : String, val spanIdHex : String, val traceFlagsHex : String, val traceStateMap : Map<String, String>) {

    constructor(spanContext: SpanContext) : this(spanContext.traceId, spanContext.spanId, spanContext.traceFlags.asHex(), spanContext.traceState.asMap().toMap())

    fun createSpanContext() = SpanContext.create(traceIdHex, spanIdHex, TraceFlags.fromHex(traceFlagsHex, 0), createTraceState())

    fun createRemoteSpanContext() = SpanContext.createFromRemoteParent(traceIdHex, spanIdHex, TraceFlags.fromHex(traceFlagsHex, 0), createTraceState())

    private fun createTraceState() = traceStateMap.toList().fold(TraceState.builder()) { builder, pair -> builder.put(pair.first, pair.second)}.build()

}

interface TelemetryService {

    fun startSpan(name: String, attributes : Map<String,String> = emptyMap(), parentSpanId : UUID? = null) : UUID
    fun endSpan(spanId: UUID)
    fun getSpanContext(spanId: UUID) : SerializableSpanContext
    fun addRemoteSpan(serializableSpanContext: SerializableSpanContext) : UUID
    fun addRemoteSpanAndStartChildSpan(serializableSpanContext: SerializableSpanContext, name: String, attributes : Map<String,String> = emptyMap()) : UUID

}