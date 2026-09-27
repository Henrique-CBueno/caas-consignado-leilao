package com.caas.auction.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WinnerSelectorTest {

    private final WinnerSelector selector = new WinnerSelector();
    private final Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void withNoBidsThereIsNoWinner() {
        Optional<Bid> winner = selector.selectWinner(List.of());

        assertThat(winner).isEmpty();
    }

    @Test
    void theLowestRateWins() {
        Bid cheaper = new Bid("funder-a", new BigDecimal("1.50"), 24, t0);
        Bid pricier = new Bid("funder-b", new BigDecimal("2.00"), 24, t0);

        Optional<Bid> winner = selector.selectWinner(List.of(pricier, cheaper));

        assertThat(winner).contains(cheaper);
    }

    @Test
    void aRateTieIsBrokenByTheShorterTerm() {
        Bid shorterTerm = new Bid("funder-a", new BigDecimal("1.50"), 12, t0);
        Bid longerTerm = new Bid("funder-b", new BigDecimal("1.50"), 24, t0);

        Optional<Bid> winner = selector.selectWinner(List.of(longerTerm, shorterTerm));

        assertThat(winner).contains(shorterTerm);
    }

    @Test
    void aRateAndTermTieIsBrokenByArrivalOrder() {
        Bid arrivedFirst = new Bid("funder-a", new BigDecimal("1.50"), 24, t0);
        Bid arrivedSecond = new Bid("funder-b", new BigDecimal("1.50"), 24, t0.plusSeconds(5));

        Optional<Bid> winner = selector.selectWinner(List.of(arrivedSecond, arrivedFirst));

        assertThat(winner).contains(arrivedFirst);
    }
}
