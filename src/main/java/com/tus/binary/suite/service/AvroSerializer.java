package com.tus.binary.suite.service;

import com.tus.binary.suite.avro.BidAskEntry;
import com.tus.binary.suite.avro.Instrument;
import com.tus.binary.suite.avro.MarketData;
import com.tus.binary.suite.dto.MarketDataPayload;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class AvroSerializer implements ProtocolSerializer {

    private final SpecificDatumWriter<MarketData> writer =
            new SpecificDatumWriter<>(MarketData.class);
    private final SpecificDatumReader<MarketData> reader =
            new SpecificDatumReader<>(MarketData.class);

    @Override
    public byte[] serialize(MarketDataPayload payload) throws IOException {
        MarketData avroRecord = toAvro(payload);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(avroRecord, encoder);
        encoder.flush();
        return out.toByteArray();
    }

    @Override
    public MarketDataPayload deserialize(byte[] data) throws IOException {
        BinaryDecoder decoder = DecoderFactory.get()
                .binaryDecoder(new ByteArrayInputStream(data), null);
        MarketData avroRecord = reader.read(null, decoder);
        return fromAvro(avroRecord);
    }

    @Override
    public String getName() {
        return "Avro";
    }

    // --- Mapping helpers ---

    private MarketData toAvro(MarketDataPayload payload) {
        Instrument avroInstrument = Instrument.newBuilder()
                .setSymbol(payload.instrument().symbol())
                .setExchange(payload.instrument().exchange())
                .setCurrency(payload.instrument().currency())
                .build();

        List<BidAskEntry> avroEntries = payload.entries().stream()
                .map(e -> BidAskEntry.newBuilder()
                        .setPrice(e.price())
                        .setSize(e.size())
                        .setLevel(e.level())
                        .setSide(e.side())
                        .setUpdateAction(e.updateAction())
                        .build())
                .collect(Collectors.toList());

        return MarketData.newBuilder()
                .setMessageType(payload.header().messageType())
                .setTimestamp(payload.header().timestamp())
                .setSequenceId(payload.header().sequenceId())
                .setVersion(payload.header().version())
                .setInstrument(avroInstrument)
                .setBidAskEntries(avroEntries)
                .build();
    }

    private MarketDataPayload fromAvro(MarketData avroRecord) {
        MarketDataPayload.Header header = new MarketDataPayload.Header(
                avroRecord.getMessageType().toString(),
                avroRecord.getTimestamp(),
                avroRecord.getSequenceId(),
                avroRecord.getVersion()
        );

        MarketDataPayload.Instrument instrument = new MarketDataPayload.Instrument(
                avroRecord.getInstrument().getSymbol().toString(),
                avroRecord.getInstrument().getExchange().toString(),
                avroRecord.getInstrument().getCurrency().toString()
        );

        List<MarketDataPayload.BidAskEntry> entries = avroRecord.getBidAskEntries()
                .stream()
                .map(e -> new MarketDataPayload.BidAskEntry(
                        e.getPrice(),
                        e.getSize(),
                        e.getLevel(),
                        e.getSide(),
                        e.getUpdateAction()
                ))
                .collect(Collectors.toList());

        return new MarketDataPayload(header, instrument, entries);
    }
}