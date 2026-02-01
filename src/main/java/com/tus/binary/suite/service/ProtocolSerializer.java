package com.tus.binary.suite.service;

import com.tus.binary.suite.dto.MarketDataPayload;

import java.io.IOException;

public interface ProtocolSerializer {
    byte[] serialize(MarketDataPayload media) throws IOException;

    MarketDataPayload deserialize(byte[] data) throws IOException;

    String getName();
}
