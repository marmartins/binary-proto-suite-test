package com.tus.binary.suite.service;

import com.google.flatbuffers.FlatBufferBuilder;
import com.tus.binary.suite.dto.MarketDataPayload;
import com.tus.binary.suite.fbs.BidAskEntry;
import com.tus.binary.suite.fbs.Instrument;
import com.tus.binary.suite.fbs.MarketData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FlatBuffersSerializer implements ProtocolSerializer {

    @Override
    public byte[] serialize(MarketDataPayload payload) throws IOException {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int msgTypeOffset = builder.createString(payload.header().messageType());

        int symbolOffset = builder.createString(payload.instrument().symbol());
        int exchangeOffset = builder.createString(payload.instrument().exchange());
        int currencyOffset = builder.createString(payload.instrument().currency());

        Instrument.startInstrument(builder);
        Instrument.addSymbol(builder, symbolOffset);
        Instrument.addExchange(builder, exchangeOffset);
        Instrument.addCurrency(builder, currencyOffset);
        int instrumentOffset = Instrument.endInstrument(builder);

        int[] entriesArr = new int[payload.entries().size()];
        for (int i = 0; i < payload.entries().size(); i++) {
            MarketDataPayload.BidAskEntry e = payload.entries().get(i);
            BidAskEntry.startBidAskEntry(builder);
            BidAskEntry.addPrice(builder, e.price());
            BidAskEntry.addSize(builder, e.size());
            BidAskEntry.addLevel(builder, e.level());
            BidAskEntry.addSide(builder, (byte)e.side());
            BidAskEntry.addUpdateAction(builder, (byte)e.updateAction());
            entriesArr[i] = BidAskEntry.endBidAskEntry(builder);
        }
        int entriesVector = MarketData.createBidAskEntriesVector(builder, entriesArr);

        MarketData.startMarketData(builder);
        MarketData.addMessageType(builder, msgTypeOffset);
        MarketData.addTimestamp(builder, payload.header().timestamp());
        MarketData.addSequenceId(builder, payload.header().sequenceId());
        MarketData.addVersion(builder, payload.header().version());
        MarketData.addInstrument(builder, instrumentOffset);
        MarketData.addBidAskEntries(builder, entriesVector);
        int mdOffset = MarketData.endMarketData(builder);

        builder.finish(mdOffset);
        return builder.sizedByteArray();
    }

    @Override
    public MarketDataPayload deserialize(byte[] data) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(data);
        MarketData md = MarketData.getRootAsMarketData(bb);

        MarketDataPayload.Header header = new MarketDataPayload.Header(
                md.messageType(),
                md.timestamp(),
                md.sequenceId(),
                md.version()
        );

        Instrument inst = md.instrument();
        MarketDataPayload.Instrument instrument = new MarketDataPayload.Instrument(
                inst.symbol(),
                inst.exchange(),
                inst.currency()
        );

        List<MarketDataPayload.BidAskEntry> entries = new ArrayList<>(md.bidAskEntriesLength());
        for (int i = 0; i < md.bidAskEntriesLength(); i++) {
            BidAskEntry e = md.bidAskEntries(i);
            entries.add(new MarketDataPayload.BidAskEntry(
                    e.price(),
                    e.size(),
                    e.level(),
                    e.side(),
                    e.updateAction()
            ));
        }

        return new MarketDataPayload(header, instrument, entries);
    }

    @Override
    public String getName() {
        return "FlatBuffers";
    }
}
