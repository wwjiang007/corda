package net.corda.node.services.telemetry

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
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
import net.corda.core.node.services.TelemetryService
import net.corda.core.serialization.SingletonSerializeAsToken
import java.util.*

class OpenTelemetryService() : SingletonSerializeAsToken(), TelemetryService {

    private val OTLP_HOST_SUPPLIER = "https://otlp.nr-data.net:4317"
    private val NEW_RELIC_API_KEY_SUPPLIER = ""

    init {
        configureOpenTelemetry()
    }

    private fun configureOpenTelemetry() {
        configureGlobal("corda")
    }

    private fun configureGlobal(defaultServiceName: String) {
        val resource = configureResource(defaultServiceName)

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

    private fun configureResource(defaultServiceName: String): Resource {
        return Resource.getDefault()
                .merge(
                        Resource.builder()
                                .put(
                                        ResourceAttributes.SERVICE_NAME,
                                        defaultServiceName
                                )
                                .put(ResourceAttributes.SERVICE_INSTANCE_ID, UUID.randomUUID().toString())
                                .build()
                )
    }

    private fun newRelicApiOrLicenseKey(): String {
        return NEW_RELIC_API_KEY_SUPPLIER
    }

}