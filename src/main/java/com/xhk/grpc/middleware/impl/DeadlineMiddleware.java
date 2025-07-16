package com.xhk.grpc.middleware.impl;

import com.xhk.grpc.middleware.Middleware;
import com.xhk.grpc.middleware.MiddlewareContext;

import java.time.Duration;

public class DeadlineMiddleware implements Middleware {
    private final Duration timeout;

    public DeadlineMiddleware(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public <ReqT, RespT> void invoke(MiddlewareContext<ReqT, RespT> context) {
        if (timeout != null) {
            context.setHeader("x-deadline-ms", String.valueOf(timeout.toMillis()));
        }
        context.next();
    }
} 