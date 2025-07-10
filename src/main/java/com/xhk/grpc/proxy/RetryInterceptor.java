package com.xhk.grpc.proxy;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

import java.util.Set;

public class RetryInterceptor implements ClientInterceptor {

    private final int maxAttempts;
    private final long initialBackoffMillis;
    private final double backoffMultiplier;
    private final Set<Status.Code> retryableCodes;

    public RetryInterceptor(int maxAttempts, long initialBackoffMillis, double backoffMultiplier, Set<Status.Code> retryableCodes) {
        this.maxAttempts = maxAttempts;
        this.initialBackoffMillis = initialBackoffMillis;
        this.backoffMultiplier = backoffMultiplier;
        this.retryableCodes = retryableCodes;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new RetryingClientCall<>(method, callOptions, next);
    }

    private class RetryingClientCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {

        private final MethodDescriptor<ReqT, RespT> method;
        private final CallOptions callOptions;
        private final Channel channel;

        public RetryingClientCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel channel) {
            this.method = method;
            this.callOptions = callOptions;
            this.channel = channel;
        }

        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
            doRetry(1, initialBackoffMillis, responseListener, headers);
        }

        private void doRetry(int attempt, long backoffMillis, Listener<RespT> responseListener, Metadata headers) {
            ClientCall<ReqT, RespT> call = channel.newCall(method, callOptions);
            call.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {
                @Override
                public void onClose(Status status, Metadata trailers) {
                    if (shouldRetry(status.getCode(), attempt)) {
                        try {
                            Thread.sleep(backoffMillis);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            super.onClose(Status.ABORTED.withDescription("Retry interrupted"), trailers);
                            return;
                        }
                        long nextBackoff = (long) (backoffMillis * backoffMultiplier);
                        doRetry(attempt + 1, nextBackoff, responseListener, headers);
                    } else {
                        super.onClose(status, trailers);
                    }
                }
            }, headers);
        }

        @Override
        public void request(int numMessages) {
        }

        @Override
        public void cancel(String message, Throwable cause) {
        }

        @Override
        public void halfClose() {
        }

        @Override
        public void sendMessage(ReqT message) {
        }
    }

    private boolean shouldRetry(Status.Code code, int attempt) {
        return attempt < maxAttempts && retryableCodes.contains(code);
    }
}
