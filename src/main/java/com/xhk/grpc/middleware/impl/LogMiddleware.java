package com.xhk.grpc.middleware.impl;

import com.xhk.grpc.middleware.Middleware;
import com.xhk.grpc.middleware.MiddlewareContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogMiddleware implements Middleware {
    private static final Logger logger = LoggerFactory.getLogger(LogMiddleware.class);

    @Override
    public <ReqT, RespT> void invoke(MiddlewareContext<ReqT, RespT> context) {
        logger.info("[gRPC] Calling with request: {}", context.getRequest());
        context.next();
        logger.info("[gRPC] Response: {}", context.getResponse());
    }
} 