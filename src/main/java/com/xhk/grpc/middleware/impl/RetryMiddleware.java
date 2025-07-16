package com.xhk.grpc.middleware.impl;

import com.xhk.grpc.middleware.Middleware;
import com.xhk.grpc.middleware.MiddlewareContext;

import java.util.Set;

public class RetryMiddleware implements Middleware {
    private final int maxAttempts;
    private final long initialBackoffMillis;
    private final double backoffMultiplier;
    private final Set<String> retryableCodes; // Sử dụng String cho status code

    public RetryMiddleware(int maxAttempts, long initialBackoffMillis, double backoffMultiplier, Set<String> retryableCodes) {
        this.maxAttempts = maxAttempts;
        this.initialBackoffMillis = initialBackoffMillis;
        this.backoffMultiplier = backoffMultiplier;
        this.retryableCodes = retryableCodes;
    }

    @Override
    public <ReqT, RespT> void invoke(MiddlewareContext<ReqT, RespT> context) {
        int attempt = 1;
        long backoff = initialBackoffMillis;
        while (attempt <= maxAttempts) {
            context.next();
            Object resp = context.getResponse();
            String code = extractStatusCode(resp);
            if (code == null || !retryableCodes.contains(code)) {
                break;
            }
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            backoff = (long) (backoff * backoffMultiplier);
            attempt++;
        }
    }

    private String extractStatusCode(Object resp) {
        // Tùy vào response thực tế, có thể cần sửa lại logic này
        if (resp instanceof MiddlewareResponseWithStatus r) {
            return r.getStatusCode();
        }
        return null;
    }

    // Interface phụ trợ nếu muốn truyền status code từ response
    public interface MiddlewareResponseWithStatus {
        String getStatusCode();
    }
} 