package com.xhk.grpc.proxy;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcLogClientInterceptor implements ClientInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(GrpcLogClientInterceptor.class);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                logger.info("[gRPC] Calling method: {}", method.getFullMethodName());

                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {
                    @Override
                    public void onMessage(RespT message) {
                        logger.info("[gRPC] Received response:\n{}", toJsonSafe(message));
                        super.onMessage(message);
                    }

                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        logger.info("[gRPC] Call closed with status: {}", status);
                        super.onClose(status, trailers);
                    }
                }, headers);
            }

            @Override
            public void sendMessage(ReqT message) {
                logger.info("[gRPC] Sending request:\n{}", toJsonSafe(message));
                super.sendMessage(message);
            }
        };
    }

    private String toJsonSafe(Object msg) {
        if (msg instanceof MessageOrBuilder protobufMessage) {
            try {
                return JsonFormat.printer()
                        .omittingInsignificantWhitespace()
                        .includingDefaultValueFields()
                        .print(protobufMessage);
            } catch (Exception e) {
                logger.warn("[gRPC] Failed to convert message to JSON: {}", e.getMessage());
            }
        }
        return msg.toString();
    }

}