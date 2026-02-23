package com.tus.binary.suite.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.tus.binary.suite.dto.MarketDataPayload;
import com.tus.binary.suite.proto.MarketData;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.stream.Collectors;

@Service
public class ProtobufSerializer implements ProtocolSerializer {

    @Override
    public byte[] serialize(MarketDataPayload payload) {
        MarketData.Builder builder = MarketData.newBuilder();

        // Header
        builder.setMessageType(payload.header().messageType())
                .setTimestamp(payload.header().timestamp())
                .setSequenceId(payload.header().sequenceId())
                .setVersion(payload.header().version());

        // Instrument
        if (payload.instrument() != null) {
            builder.setInstrument(MarketData.Instrument.newBuilder()
                    .setSymbol(payload.instrument().symbol())
                    .setExchange(payload.instrument().exchange())
                    .setCurrency(payload.instrument().currency())
                    .build());
        }

        // Entries
        if (payload.entries() != null) {
            for (MarketDataPayload.BidAskEntry entry : payload.entries()) {
                MarketData.BidAskEntry.Builder entryBuilder = MarketData.BidAskEntry.newBuilder()
                        .setPrice(entry.price()) // int64
                        .setSize(entry.size())
                        .setLevel(entry.level())
                        .setSideValue(entry.side())
                        .setUpdateActionValue(entry.updateAction());
                builder.addBidAskEntries(entryBuilder);
            }
        }

        return builder.build().toByteArray();
    }

    @Override
    public MarketDataPayload deserialize(byte[] data) throws IOException {
        try {
            MarketData proto = MarketData.parseFrom(data);

            MarketDataPayload.Header header = new MarketDataPayload.Header(
                    proto.getMessageType(),
                    proto.getTimestamp(),
                    proto.getSequenceId(),
                    proto.getVersion());

            MarketDataPayload.Instrument instrument = null;
            if (proto.hasInstrument()) {
                instrument = new MarketDataPayload.Instrument(
                        proto.getInstrument().getSymbol(),
                        proto.getInstrument().getExchange(),
                        proto.getInstrument().getCurrency());
            }

            var entries = proto.getBidAskEntriesList().stream()
                    .map(e -> new MarketDataPayload.BidAskEntry(
                            e.getPrice(),
                            e.getSize(),
                            e.getLevel(),
                            e.getSideValue(),
                            e.getUpdateActionValue()))
                    .collect(Collectors.toList());

            return new MarketDataPayload(header, instrument, entries);
        } catch (InvalidProtocolBufferException e) {
            throw new IOException("Failed to deserialize Protobuf", e);
        }
    }

    @Override
    public String getName() {
        return "Protobuf";
    }
}
