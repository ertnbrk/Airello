package ai.planmate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import lombok.extern.slf4j.Slf4j;

/**
 * Observability Configuration with Metrics and Distributed Tracing.
 *
 * <p><b>METRICS:</b> Prometheus-compatible metrics for monitoring.
 *
 * <p><b>TRACING:</b> OpenTelemetry + Zipkin for distributed tracing.
 *
 * <p><b>TRACE FLOW EXAMPLE:</b>
 *
 * <pre>
 * HTTP Request → Controller → Service → DB → Redis Queue → AI Worker
 *                                                  ↓
 *                                           Single TraceID
 * </pre>
 */
@Slf4j
@Configuration
public class ObservabilityConfig {

    @Value("${spring.application.name:airello}")
    private String applicationName;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${otel.zipkin.endpoint:#{null}}")
    private String zipkinEndpoint;

    @Value("${otel.tracing.enabled:true}")
    private boolean tracingEnabled;

    // ========================================
    // Metrics (Prometheus)
    // ========================================

    @Bean
    public Timer aiRequestDurationTimer(MeterRegistry registry) {
        return Timer.builder("ai.request.duration")
                .description("AI request processing duration")
                .tag("type", "ai")
                .register(registry);
    }

    @Bean
    public Counter aiRequestFailuresCounter(MeterRegistry registry) {
        return Counter.builder("ai.request.failures")
                .description("Total AI request failures")
                .tag("type", "ai")
                .register(registry);
    }

    @Bean
    public Counter artifactUploadCounter(MeterRegistry registry) {
        return Counter.builder("artifact.uploads")
                .description("Total artifact uploads")
                .tag("type", "storage")
                .register(registry);
    }

    // ========================================
    // Distributed Tracing (OpenTelemetry + Zipkin)
    // ========================================

    /**
     * Configures OpenTelemetry SDK with Zipkin exporter.
     *
     * @return Configured OpenTelemetry instance
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        if (!tracingEnabled) {
            log.warn("⚠️  OpenTelemetry tracing is DISABLED");
            return OpenTelemetry.noop();
        }

        // Define service resource attributes
        var resource =
                Resource.getDefault()
                        .merge(
                                Resource.create(
                                        Attributes.builder()
                                                .put(
                                                        ResourceAttributes.SERVICE_NAME,
                                                        applicationName)
                                                .put(ResourceAttributes.SERVICE_VERSION, "1.0.0")
                                                .put(
                                                        ResourceAttributes.DEPLOYMENT_ENVIRONMENT,
                                                        activeProfile)
                                                .build()));

        // Configure span exporter (Zipkin) - only if endpoint is configured
        var tracerProviderBuilder = SdkTracerProvider.builder().setResource(resource);

        if (zipkinEndpoint != null && !zipkinEndpoint.isEmpty()) {
            try {
                var zipkinExporter =
                        ZipkinSpanExporter.builder().setEndpoint(zipkinEndpoint).build();
                tracerProviderBuilder.addSpanProcessor(
                        BatchSpanProcessor.builder(zipkinExporter).build());
                log.info("✅ Zipkin exporter configured: { }", zipkinEndpoint);
            } catch (Exception e) {
                log.warn("⚠️  Zipkin exporter failed to initialize: { }", e.getMessage());
            }
        } else {
            log.info("ℹ️  Zipkin exporter disabled (no endpoint configured)");
        }

        var tracerProvider = tracerProviderBuilder.build();

        // Build OpenTelemetry SDK
        var openTelemetry =
                OpenTelemetrySdk.builder()
                        .setTracerProvider(tracerProvider)
                        .setPropagators(
                                ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                        .buildAndRegisterGlobal();

        log.info("🚀 OpenTelemetry SDK initialized:");
        log.info("   - Service: { }", applicationName);
        log.info("   - Environment: { }", activeProfile);
        log.info("   - Trace Context: W3C (traceparent header)");
        log.info("   - Zipkin UI: http://localhost:9411/zipkin/");

        // Register shutdown hook
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    try {
                                        tracerProvider.close();
                                        log.info("OpenTelemetry tracer provider shut down");
                                    } catch (Exception e) {
                                        log.error("Error shutting down OpenTelemetry", e);
                                    }
                                }));

        return openTelemetry;
    }

    /**
     * Creates Micrometer Tracer from OpenTelemetry.
     *
     * @param openTelemetry OpenTelemetry instance
     * @return Micrometer Tracer
     */
    @Bean
    public Tracer micrometerTracer(OpenTelemetry openTelemetry) {
        var otelTracer = openTelemetry.getTracer("io.micrometer.micrometer-tracing");
        return new OtelTracer(otelTracer, new OtelCurrentTraceContext(), null);
    }

    /**
     * Enables {@code @Observed} annotation support.
     *
     * @param observationRegistry Observation registry
     * @return ObservedAspect instance
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
