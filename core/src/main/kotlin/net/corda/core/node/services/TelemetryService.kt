package net.corda.core.node.services

import java.util.*

interface TelemetryService {

    fun startSpan(name: String, attributes : Map<String,String> = emptyMap(), parentSpanId : UUID? = null) : UUID
    fun endSpan(spanId: UUID)

}