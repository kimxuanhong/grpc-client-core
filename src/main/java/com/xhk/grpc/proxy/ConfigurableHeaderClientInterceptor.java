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

public class ConfigurableHeaderClientInterceptor implements ClientInterceptor {
    private final Map<String, String> headers;

    public ConfigurableHeaderClientInterceptor(String[] headerConfigs) {
        this.headers = parseHeaders(headerConfigs);
    }
    
    public ConfigurableHeaderClientInterceptor() {
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
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                for (Map.Entry<String, String> header : ConfigurableHeaderClientInterceptor.this.headers.entrySet()) {
                    headers.put(Metadata.Key.of(header.getKey(), Metadata.ASCII_STRING_MARSHALLER), header.getValue());
                }
                super.start(responseListener, headers);
            }
        };
    }
} 