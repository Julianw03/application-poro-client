package com.iambadatplaying.ressourceServer;

import java.util.List;
import java.util.Map;

public class ByteHeaderData {
    private final byte[] data;
    private final Map<String, List<String>> headers;

    public ByteHeaderData(byte[] data, Map<String, List<String>> headers) {
        this.data = data;
        this.headers = headers;
    }

    public byte[] getData() {
        return data;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }
}
