package com.xhk.grpc.middleware.impl;

import com.xhk.grpc.middleware.Middleware;
import com.xhk.grpc.middleware.MiddlewareContext;

import java.util.Map;
import java.util.function.Supplier;

public class DynamicHeaderMiddleware implements Middleware {
    private final Supplier<Map<String, String>> headersSupplier;

    public DynamicHeaderMiddleware(Supplier<Map<String, String>> headersSupplier) {
        this.headersSupplier = headersSupplier;
    }

    @Override
    public <ReqT, RespT> void invoke(MiddlewareContext<ReqT, RespT> context) {
        Map<String, String> dynamicHeaders = headersSupplier.get();
        if (dynamicHeaders != null) {
            for (Map.Entry<String, String> entry : dynamicHeaders.entrySet()) {
                context.setHeader(entry.getKey(), entry.getValue());
            }
        }
        context.next();
    }
} 