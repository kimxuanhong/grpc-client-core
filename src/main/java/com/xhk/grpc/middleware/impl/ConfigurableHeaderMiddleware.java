package com.xhk.grpc.middleware.impl;

import com.xhk.grpc.middleware.Middleware;
import com.xhk.grpc.middleware.MiddlewareContext;

import java.util.HashMap;
import java.util.Map;

public class ConfigurableHeaderMiddleware implements Middleware {
    private final Map<String, String> headers;

    public ConfigurableHeaderMiddleware(String[] headerConfigs) {
        this.headers = parseHeaders(headerConfigs);
    }
    public ConfigurableHeaderMiddleware() {
        this.headers = new HashMap<>();
    }
    private Map<String, String> parseHeaders(String[] headerConfigs) {
        Map<String, String> headerMap = new HashMap<>();
        if (headerConfigs != null) {
            for (String config : headerConfigs) {
                String[] parts = config.split("=", 2);
                if (parts.length == 2) {
                    headerMap.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return headerMap;
    }
    @Override
    public <ReqT, RespT> void invoke(MiddlewareContext<ReqT, RespT> context) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            context.setHeader(header.getKey(), header.getValue());
        }
        context.next();
    }
} 