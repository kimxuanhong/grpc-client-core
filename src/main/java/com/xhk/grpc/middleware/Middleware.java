package com.xhk.grpc.middleware;

public interface Middleware {
    <ReqT, RespT> void invoke(MiddlewareContext<ReqT, RespT> context);
} 