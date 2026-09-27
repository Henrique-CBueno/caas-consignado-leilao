package com.caas.auction.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class WinnerSelector {

    private static final Comparator<Bid> TIE_BREAK_ORDER =
        Comparator.comparing(Bid::rate)
            .thenComparing(Bid::termMonths)
            .thenComparing(Bid::receivedAt);

    public Optional<Bid> selectWinner(List<Bid> bids) {
        return bids.stream().min(TIE_BREAK_ORDER);
    }
}
