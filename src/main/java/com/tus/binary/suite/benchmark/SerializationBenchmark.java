package com.tus.binary.suite.benchmark;

import com.tus.binary.suite.dto.MarketDataPayload;
import com.tus.binary.suite.service.ProtobufSerializer;
import com.tus.binary.suite.service.SbeSerializer;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SerializationBenchmark {

    private MarketDataPayload payload;
    private ProtobufSerializer protobufSerializer;
    private SbeSerializer sbeSerializer;
    private byte[] protobufEncoded;
    private byte[] sbeEncoded;

    @Setup(Level.Trial)
    public void setup() {
        protobufSerializer = new ProtobufSerializer();
        sbeSerializer = new SbeSerializer();

        payload = new MarketDataPayload(
                new MarketDataPayload.Header("MarketDataIncrementalRefresh", 1678899887123L, 1001L, 1),
                new MarketDataPayload.Instrument("MSFT", "NASDAQ", "USD"),
                List.of(
                        new MarketDataPayload.BidAskEntry(25000L, 100L, 1, 0, 0),
                        new MarketDataPayload.BidAskEntry(25010L, 50L, 1, 1, 0),
                        new MarketDataPayload.BidAskEntry(24990L, 200L, 2, 0, 0),
                        new MarketDataPayload.BidAskEntry(25020L, 150L, 2, 1, 0)));

        protobufEncoded = protobufSerializer.serialize(payload);
        sbeEncoded = sbeSerializer.serialize(payload);
    }

    @Benchmark
    public byte[] protobufSerialize() throws IOException {
        return protobufSerializer.serialize(payload);
    }

    @Benchmark
    public MarketDataPayload protobufDeserialize() throws IOException {
        return protobufSerializer.deserialize(protobufEncoded);
    }

    @Benchmark
    public byte[] sbeSerialize() throws IOException {
        return sbeSerializer.serialize(payload);
    }

    @Benchmark
    public MarketDataPayload sbeDeserialize() throws IOException {
        return sbeSerializer.deserialize(sbeEncoded);
    }
}
