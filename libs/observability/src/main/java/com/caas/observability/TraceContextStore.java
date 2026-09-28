package com.caas.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.util.HashMap;
import java.util.Map;

// Outbox publica em outra thread, depois do commit: sem guardar o traceparent junto
// do evento, cada publicação abriria um trace novo (ADR-0011).
public class TraceContextStore {

    private static final String TRACEPARENT = "traceparent";

    private final Tracer tracer;
    private final Propagator propagator;

    public TraceContextStore(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    public String capture() {
        TraceContext context = tracer.currentTraceContext().context();
        if (context == null) {
            return null;
        }
        Map<String, String> carrier = new HashMap<>();
        propagator.inject(context, carrier, Map::put);
        return carrier.get(TRACEPARENT);
    }

    public void inSpan(String traceparent, String name, Runnable body) {
        Span.Builder builder = traceparent == null
            ? tracer.spanBuilder()
            : propagator.extract(Map.of(TRACEPARENT, traceparent), Map::get);
        Span span = builder.name(name).start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            body.run();
        } catch (RuntimeException e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
