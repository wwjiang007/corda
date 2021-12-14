package net.corda.node.services.telemetry

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.internal.RetryPolicy
import io.opentelemetry.exporter.otlp.internal.grpc.DefaultGrpcExporterBuilder
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import net.corda.core.node.services.SerializableSpanContext
import net.corda.core.node.services.TelemetryService
import net.corda.core.serialization.SingletonSerializeAsToken
import java.util.*

class OpenTelemetryService(serviceName : String) : SingletonSerializeAsToken(), TelemetryService {

    private val OTLP_HOST_SUPPLIER = "http://localhost:4317"
    private val NEW_RELIC_API_KEY_SUPPLIER = ""
    private val spans = mutableMapOf<UUID, Span>()

    init {
        configureOpenTelemetry(serviceName)
    }

    override fun startSpan(name: String, attributes : Map<String,String>, parentSpanId : UUID?) : UUID {
        val attributesMap = attributes.toList().fold(Attributes.builder()) { builder, attribute -> builder.put(attribute.first, attribute.second) }.build()
        val tracer = GlobalOpenTelemetry.getTracerProvider().get(OpenTelemetryService::class.java.name)
        val spanBuilder = tracer.spanBuilder(name).setAllAttributes(attributesMap)
        val span = if (parentSpanId == null) {
            spanBuilder.setNoParent()
        } else {
            val parentSpan = spans[parentSpanId] ?: throw IllegalArgumentException("Couldn't find a span for id ${parentSpanId}")
            spanBuilder.setParent(Context.current().with(parentSpan))
        }.startSpan()

        val spanId = UUID.randomUUID()
        spans[spanId] = span
        return spanId
    }

    override fun endSpan(spanId : UUID) {
        spans[spanId]?.end()
    }

    override fun getSpanContext(spanId: UUID): SerializableSpanContext {
        val spanContext = spans[spanId]?.spanContext ?: throw IllegalArgumentException("Couldn't find a span for id ${spanId}")
        return SerializableSpanContext(spanContext)
    }

    private fun configureOpenTelemetry(serviceName : String) {
        configureGlobal(serviceName)
    }

    private fun configureGlobal(serviceName: String) {
        val resource = configureResource(serviceName)

        // Configure traces
        val spanExporterBuilder = OtlpGrpcSpanExporter.builder()
                .setEndpoint(OTLP_HOST_SUPPLIER)
                .addHeader("api-key", newRelicApiOrLicenseKey())

        // Enable retry policy via unstable API
        DefaultGrpcExporterBuilder.getDelegateBuilder(
                OtlpGrpcSpanExporterBuilder::class.java, spanExporterBuilder
        )
                .addRetryPolicy(RetryPolicy.getDefault())
        val sdkTracerProviderBuilder = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporterBuilder.build()).build())
        OpenTelemetrySdk.builder()
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .setTracerProvider(sdkTracerProviderBuilder.build())
                .buildAndRegisterGlobal()
    }

    private fun configureResource(serviceName: String): Resource {
        return Resource.getDefault()
                .merge(
                        Resource.builder()
                                .put(
                                        ResourceAttributes.SERVICE_NAME,
                                        serviceName
                                )
                                .put(ResourceAttributes.SERVICE_INSTANCE_ID, UUID.randomUUID().toString())
                                .build()
                )
    }

    private fun newRelicApiOrLicenseKey(): String {
        return NEW_RELIC_API_KEY_SUPPLIER
    }

}