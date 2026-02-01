package com.tus.binary.suite.service;

import com.tus.binary.suite.dto.MarketDataPayload;
import com.tus.binary.suite.sbe.*;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SbeSerializer implements ProtocolSerializer {

    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    private final MarketDataEncoder marketDataEncoder = new MarketDataEncoder();
    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final MarketDataDecoder marketDataDecoder = new MarketDataDecoder();

    // Buffer for serialization (expandable)
    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(1024);

    @Override
    public byte[] serialize(MarketDataPayload payload) {
        // Encode Header
        messageHeaderEncoder.wrap(buffer, 0);
        messageHeaderEncoder.blockLength(marketDataEncoder.sbeBlockLength())
                .templateId(marketDataEncoder.sbeTemplateId())
                .schemaId(marketDataEncoder.sbeSchemaId())
                .version(marketDataEncoder.sbeSchemaVersion());

        int offset = messageHeaderEncoder.encodedLength();
        marketDataEncoder.wrap(buffer, offset);

        marketDataEncoder.timestamp(payload.header().timestamp())
                .sequenceId(payload.header().sequenceId())
                .version(payload.header().version());

        // set fixed fields:
        marketDataEncoder.timestamp(payload.header().timestamp());
        marketDataEncoder.sequenceId(payload.header().sequenceId());
        marketDataEncoder.version(payload.header().version());

        // Groups (bidAskEntries)
        var entries = payload.entries();
        if (entries != null) {
            MarketDataEncoder.BidAskEntriesEncoder groupEncoder = marketDataEncoder.bidAskEntriesCount(entries.size());
            for (MarketDataPayload.BidAskEntry entry : entries) {
                groupEncoder.next();
                groupEncoder.price(entry.price())
                        .size(entry.size())
                        .level(entry.level())
                        .side(entry.side() == 0 ? Side.BID : Side.ASK)
                        .updateAction(entry.updateAction() == 0 ? UpdateAction.ADD
                                : (entry.updateAction() == 1 ? UpdateAction.UPDATE : UpdateAction.DELETE));
            }
        } else {
            marketDataEncoder.bidAskEntriesCount(0);
        }

        // Var Strings (Order in Schema matters! My schema: messageType, then symbol, exchange, currency)
        // I'll write them in the order they appear as var strings in schema
        // messageType (id 1), symbol (5), exchange (6), currency (7).

        marketDataEncoder.messageType(payload.header().messageType());
        if (payload.instrument() != null) {
            marketDataEncoder.symbol(payload.instrument().symbol());
            marketDataEncoder.exchange(payload.instrument().exchange());
            marketDataEncoder.currency(payload.instrument().currency());
        } else {
            marketDataEncoder.symbol("");
            marketDataEncoder.exchange("");
            marketDataEncoder.currency("");
        }

        int length = offset + marketDataEncoder.encodedLength();
        byte[] result = new byte[length];
        buffer.getBytes(0, result);
        return result;
    }

    @Override
    public MarketDataPayload deserialize(byte[] data) {
        UnsafeBuffer directBuffer = new UnsafeBuffer(data);
        messageHeaderDecoder.wrap(directBuffer, 0);

        int templateId = messageHeaderDecoder.templateId();
        int version = messageHeaderDecoder.version();
        int blockLength = messageHeaderDecoder.blockLength();
        int offset = messageHeaderDecoder.encodedLength();

        marketDataDecoder.wrap(directBuffer, offset, blockLength, version);

        long timestamp = marketDataDecoder.timestamp();
        long sequenceId = marketDataDecoder.sequenceId();
        // sequenceId is int64 (long) in schema

        // My body field `version` is int32 (SchemaVersion primitive int32).
        int bodyVersion = marketDataDecoder.version();

        // Groups
        List<MarketDataPayload.BidAskEntry> entries = new ArrayList<>();
        MarketDataDecoder.BidAskEntriesDecoder groupDecoder = marketDataDecoder.bidAskEntries();
        while (groupDecoder.hasNext()) {
            groupDecoder.next();
            entries.add(new MarketDataPayload.BidAskEntry(
                    groupDecoder.price(),
                    groupDecoder.size(),
                    groupDecoder.level(),
                    groupDecoder.side() == Side.BID ? 0 : 1,
                    groupDecoder.updateAction() == UpdateAction.ADD ? 0
                            : (groupDecoder.updateAction() == UpdateAction.UPDATE ? 1 : 2)));
        }

        // Var Data
        // Order must match write order / schema order.
        // messageType, symbol, exchange, currency.
        String msgType = marketDataDecoder.messageType();
        String symbol = marketDataDecoder.symbol();
        String exchange = marketDataDecoder.exchange();
        String currency = marketDataDecoder.currency();

        MarketDataPayload.Header header = new MarketDataPayload.Header(
                msgType, timestamp, sequenceId, bodyVersion);
        MarketDataPayload.Instrument instrument = new MarketDataPayload.Instrument(
                symbol, exchange, currency);

        return new MarketDataPayload(header, instrument, entries);
    }

    @Override
    public String getName() {
        return "SBE";
    }
}
