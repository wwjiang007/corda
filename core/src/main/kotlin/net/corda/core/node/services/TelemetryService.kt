package net.corda.core.node.services

import io.opentelemetry.api.trace.Span

interface TelemetryService {

    fun startSpan() : Span
    fun endSpan(span: Span)


}