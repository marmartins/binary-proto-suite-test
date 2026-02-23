package com.tus.binary.suite;

import com.tus.binary.suite.dto.MarketDataPayload;
import com.tus.binary.suite.service.ProtobufSerializer;
import com.tus.binary.suite.service.ProtocolSerializer;
import com.tus.binary.suite.service.SbeSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BinaryProtocolTest {

    @Autowired
    private ProtobufSerializer protobufSerializer;

    @Autowired
    private SbeSerializer sbeSerializer;

    @Test
    public void testProtobufRoundTrip() throws IOException {
        testRoundTrip(protobufSerializer);
    }

    @Test
    public void testSbeRoundTrip() throws IOException {
        testRoundTrip(sbeSerializer);
    }

    private void testRoundTrip(ProtocolSerializer serializer) throws IOException {
        MarketDataPayload original = createSamplePayload();
        byte[] encoded = serializer.serialize(original);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);

        MarketDataPayload decoded = serializer.deserialize(encoded);
        assertEquals(original, decoded, "Decoded object should match original for " + serializer.getName());

        System.out.println(serializer.getName() + " encoded size: " + encoded.length + " bytes");
    }

    private MarketDataPayload createSamplePayload() {
        return new MarketDataPayload(
                new MarketDataPayload.Header("MarketDataIncrementalRefresh", System.nanoTime(), 12345L, 1),
                new MarketDataPayload.Instrument("AAPL", "NASDAQ", "USD"),
                List.of(
                        new MarketDataPayload.BidAskEntry(15000L, 100L, 1, 0, 0), // Bid
                        new MarketDataPayload.BidAskEntry(15100L, 200L, 1, 1, 0) // Ask
                ));
    }
}
