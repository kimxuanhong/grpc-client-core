package com.xhk.grpc.factory;

import java.util.Map;

public class GrpcClientConfig {
    private final String url;
    private final long timeoutMs;
    private final Map<String, String> headers;

    public GrpcClientConfig(String url, long timeoutMs, Map<String, String> headers) {
        this.url = url;
        this.timeoutMs = timeoutMs;
        this.headers = headers;
    }

    public String getUrl() {
        return url;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}

