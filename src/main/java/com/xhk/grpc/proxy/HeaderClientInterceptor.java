package com.xhk.grpc.proxy;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HeaderClientInterceptor implements ClientInterceptor {
    private final Map<String, String> props;

    public HeaderClientInterceptor(Map<String, String> headers) {
        this.props = Optional.ofNullable(headers).orElseGet(HashMap::new);
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                for (Map.Entry<String, String> header : props.entrySet()) {
                    headers.put(Metadata.Key.of(header.getKey(), Metadata.ASCII_STRING_MARSHALLER), header.getValue());
                }
                super.start(responseListener, headers);
            }
        };
    }
}

