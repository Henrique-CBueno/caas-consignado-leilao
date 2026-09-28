package com.caas.funderbot.infrastructure.kafka;

import com.caas.events.AuctionOpenedEvent;
import com.caas.funderbot.infrastructure.client.BidSubmitter;
import com.caas.funderbot.infrastructure.config.FunderBotProperties;
import com.caas.observability.TraceContextStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuctionOpenedListener {

    // Variação de taxa em torno da taxa-base do bot — heurística simples de mercado,
    // não baseada em risco real (tenant-service ainda não modela isso, ver spec Milestone 5/7).
    private static final double RATE_JITTER = 0.2;

    private final FunderBotProperties properties;
    private final BidSubmitter bidSubmitter;
    private final ObjectMapper objectMapper;
    private final TraceContextStore traceContextStore;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Random random = new Random();

    public AuctionOpenedListener(
        FunderBotProperties properties,
        BidSubmitter bidSubmitter,
        ObjectMapper objectMapper,
        TraceContextStore traceContextStore
    ) {
        this.traceContextStore = traceContextStore;
        this.properties = properties;
        this.bidSubmitter = bidSubmitter;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "auction.opened")
    public void onAuctionOpened(String payload) throws Exception {
        AuctionOpenedEvent event = objectMapper.readValue(payload, AuctionOpenedEvent.class);
        // O lance roda em outra thread (delay aleatório): o contexto do listener precisa ser levado junto.
        String traceparent = traceContextStore.capture();

        for (FunderBotProperties.Bot bot : properties.bots()) {
            if (!event.eligibleFunderIds().contains(bot.id())) {
                continue;
            }
            long delayMs = properties.minDelayMs()
                + (long) (random.nextDouble() * (properties.maxDelayMs() - properties.minDelayMs()));
            BigDecimal rate = jitteredRate(bot.baseRate());

            scheduler.schedule(
                () -> traceContextStore.inSpan(
                    traceparent, "submit-bid " + bot.id(),
                    () -> bidSubmitter.submitBid(event.proposalId(), bot.id(), rate, bot.termMonths())
                ),
                delayMs, TimeUnit.MILLISECONDS
            );
        }
    }

    private BigDecimal jitteredRate(BigDecimal baseRate) {
        double jitter = (random.nextDouble() - 0.5) * RATE_JITTER;
        return baseRate.add(BigDecimal.valueOf(jitter)).setScale(2, RoundingMode.HALF_UP);
    }
}
