package com.xhk.grpc.factory;

import com.xhk.grpc.utils.EnvUtils;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.Optional;

public class GrpcClientConfigLoader {
    public static GrpcClientConfig load(Class<?> clientInterface, Environment env, String overrideUrl) {
        String key = "grpc.clients." + clientInterface.getSimpleName();
        String url = (overrideUrl != null && !overrideUrl.isBlank())
                ? overrideUrl
                : env.getProperty(key + ".url", "localhost:9090");

        long timeoutMs = Optional.ofNullable(env.getProperty(key + ".default-timeout-ms"))
                .map(val -> {
                    try { return Long.parseLong(val); }
                    catch (NumberFormatException e) { return 5000L; }
                }).orElse(5000L);

        Map<String, String> headers = EnvUtils.getClientProperties(key + ".headers", env);
        return new GrpcClientConfig(url, timeoutMs, headers);
    }
}
