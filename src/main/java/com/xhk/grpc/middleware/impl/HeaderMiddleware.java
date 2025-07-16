package com.xhk.grpc.middleware.impl;

import com.xhk.grpc.middleware.Middleware;
import com.xhk.grpc.middleware.MiddlewareContext;

import java.util.Map;

public class HeaderMiddleware implements Middleware {
    private final Map<String, String> headers;

    public HeaderMiddleware(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public <ReqT, RespT> void invoke(MiddlewareContext<ReqT, RespT> context) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                context.setHeader(entry.getKey(), entry.getValue());
            }
        }
        context.next();
    }
} 