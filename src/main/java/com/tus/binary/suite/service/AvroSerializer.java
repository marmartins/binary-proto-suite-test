package com.tus.binary.suite.service;

import com.tus.binary.suite.avro.MarketData;
import com.tus.binary.suite.avro.Header;
import com.tus.binary.suite.avro.Instrument;
import com.tus.binary.suite.avro.BidAskEntry;
import com.tus.binary.suite.dto.MarketDataPayload;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AvroSerializer implements ProtocolSerializer {

    private final SpecificDatumWriter<MarketData> writer;
    private final SpecificDatumReader<MarketData> reader;

    public AvroSerializer() {
        this.writer = new SpecificDatumWriter<>(MarketData.class);
        this.reader = new SpecificDatumReader<>(MarketData.class);
    }

    @Override
    public byte[] serialize(MarketDataPayload payload) throws IOException {
        Header header = Header.newBuilder()
                .setMessageType(payload.header().messageType())
                .setTimestamp(payload.header().timestamp())
                .setSequenceId(payload.header().sequenceId())
                .setVersion(payload.header().version())
                .build();

        Instrument instrument = Instrument.newBuilder()
                .setSymbol(payload.instrument().symbol())
                .setExchange(payload.instrument().exchange())
                .setCurrency(payload.instrument().currency())
                .build();

        List<BidAskEntry> entries = new ArrayList<>();
        for (MarketDataPayload.BidAskEntry entry : payload.entries()) {
            entries.add(BidAskEntry.newBuilder()
                    .setPrice(entry.price())
                    .setSize(entry.size())
                    .setLevel(entry.level())
                    .setSide(entry.side())
                    .setUpdateAction(entry.updateAction())
                    .build());
        }

        MarketData marketData = MarketData.newBuilder()
                .setHeader(header)
                .setInstrument(instrument)
                .setEntries(entries)
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(marketData, encoder);
        encoder.flush();
        return out.toByteArray();
    }

    @Override
    public MarketDataPayload deserialize(byte[] data) throws IOException {
        Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        MarketData marketData = reader.read(null, decoder);

        Header header = marketData.getHeader();
        MarketDataPayload.Header dtoHeader = new MarketDataPayload.Header(
                header.getMessageType(),
                header.getTimestamp(),
                header.getSequenceId(),
                header.getVersion()
        );

        Instrument instrument = marketData.getInstrument();
        MarketDataPayload.Instrument dtoInstrument = new MarketDataPayload.Instrument(
                instrument.getSymbol(),
                instrument.getExchange(),
                instrument.getCurrency()
        );

        List<MarketDataPayload.BidAskEntry> dtoEntries = new ArrayList<>();
        for (BidAskEntry entry : marketData.getEntries()) {
            dtoEntries.add(new MarketDataPayload.BidAskEntry(
                    entry.getPrice(),
                    entry.getSize(),
                    entry.getLevel(),
                    entry.getSide(),
                    entry.getUpdateAction()
            ));
        }

        return new MarketDataPayload(dtoHeader, dtoInstrument, dtoEntries);
    }

    @Override
    public String getName() {
        return "Avro";
    }
}
