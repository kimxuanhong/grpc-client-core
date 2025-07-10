package com.xhk.grpc.proxy;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class DeadlineInterceptor implements ClientInterceptor {
    private final Duration timeout;

    public DeadlineInterceptor(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        if (callOptions.getDeadline() == null && timeout != null) {
            callOptions = callOptions.withDeadlineAfter(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        return next.newCall(method, callOptions);
    }
}

