package com.caas.observability;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ObservabilityAutoConfiguration {

    @Bean
    TraceContextStore traceContextStore(Tracer tracer, Propagator propagator) {
        return new TraceContextStore(tracer, propagator);
    }
}
