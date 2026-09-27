package com.caas.credit.infrastructure.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.caas.credit.application.CreditDecisionPort;
import com.caas.credit.infrastructure.decision.jev.JevOpenRouterAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CreditDecisionProviderSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(MockDecisionAdapter.class, JevOpenRouterAdapter.class)
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withBean(CircuitBreakerRegistry.class, CircuitBreakerRegistry::ofDefaults)
        .withBean(RateLimiterRegistry.class, RateLimiterRegistry::ofDefaults)
        .withPropertyValues("app.jev.decisions-url=http://localhost:1", "app.jev.api-key=unused");

    @Test
    void defaultsToTheMockAdapterWhenNoProviderIsConfigured() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CreditDecisionPort.class);
            assertThat(context.getBean(CreditDecisionPort.class)).isInstanceOf(MockDecisionAdapter.class);
        });
    }

    @Test
    void usesTheJevAdapterWhenExplicitlyConfigured() {
        contextRunner
            .withPropertyValues("app.credit-decision.provider=jev")
            .run(context -> {
                assertThat(context).hasSingleBean(CreditDecisionPort.class);
                assertThat(context.getBean(CreditDecisionPort.class)).isInstanceOf(JevOpenRouterAdapter.class);
            });
    }
}
