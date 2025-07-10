package com.xhk.grpc.proxy;

import io.grpc.*;

import java.util.Map;
import java.util.function.Supplier;

public class DynamicHeaderClientInterceptor implements ClientInterceptor {

    private final Supplier<Map<String, String>> headersSupplier;

    public DynamicHeaderClientInterceptor(Supplier<Map<String, String>> headersSupplier) {
        this.headersSupplier = headersSupplier;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                Map<String, String> dynamicHeaders = headersSupplier.get();
                if (dynamicHeaders != null) {
                    for (Map.Entry<String, String> entry : dynamicHeaders.entrySet()) {
                        headers.put(
                                Metadata.Key.of(entry.getKey(), Metadata.ASCII_STRING_MARSHALLER),
                                entry.getValue()
                        );
                    }
                }
                super.start(responseListener, headers);
            }
        };
    }
}
