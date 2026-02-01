package com.tus.binary.suite.dto;

import java.util.List;

public record MarketDataPayload(
                Header header,
                Instrument instrument,
                List<BidAskEntry> entries) {

        public record Header(
                        String messageType,
                        long timestamp,
                        long sequenceId,
                        int version) {
        }

        public record Instrument(
                        String symbol,
                        String exchange,
                        String currency) {
        }

        public record BidAskEntry(
                        long price,
                        long size,
                        int level,
                        int side, // 0=BID, 1=ASK
                        int updateAction // 0=ADD, 1=UPDATE, 2=DELETE
        ) {
        }

        public static MarketDataPayload createSample() {
                return new MarketDataPayload(
                                new Header("MarketDataIncrementalRefresh", 1678899887123L, 1001L, 1),
                                new Instrument("MSFT", "NASDAQ", "USD"),
                                List.of(
                                                new BidAskEntry(25000L, 100L, 1, 0, 0),
                                                new BidAskEntry(25010L, 50L, 1, 1, 0),
                                                new BidAskEntry(24990L, 200L, 2, 0, 0),
                                                new BidAskEntry(25020L, 150L, 2, 1, 0)));
        }
}
