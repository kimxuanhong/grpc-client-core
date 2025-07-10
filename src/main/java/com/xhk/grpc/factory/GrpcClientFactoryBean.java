package com.xhk.grpc.factory;


import com.xhk.grpc.proxy.DeadlineInterceptor;
import com.xhk.grpc.proxy.GrpcLogClientInterceptor;
import com.xhk.grpc.proxy.HeaderClientInterceptor;
import com.xhk.grpc.utils.EnvUtils;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;

public class GrpcClientFactoryBean<T> implements FactoryBean<T>, EnvironmentAware {
    private final Class<T> clientInterface;
    private final Class<?> stubClass;
    private Environment environment;
    private String url;
    private final Class<? extends ClientInterceptor>[] interceptorClasses;

    public GrpcClientFactoryBean(Class<T> clientInterface, Class<?> stubClass, Class<? extends ClientInterceptor>[] interceptorClasses) {
        this.clientInterface = clientInterface;
        this.stubClass = stubClass;
        this.interceptorClasses = interceptorClasses;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public T getObject() throws Exception {
        String key = "grpc.clients." + clientInterface.getSimpleName();
        String url = (this.url != null && !this.url.isBlank()) ? this.url : environment.getProperty(key + ".url", "localhost:9090");

        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forTarget(url)
                .usePlaintext();

        long defaultTimeout = Long.parseLong(environment.getProperty(key + ".default-timeout-ms", "5000"));
        channelBuilder.intercept(new DeadlineInterceptor(Duration.ofMillis(defaultTimeout)));

        channelBuilder.intercept(new GrpcLogClientInterceptor());
        if (interceptorClasses != null) {
            for (Class<? extends ClientInterceptor> clazz : interceptorClasses) {
                channelBuilder.intercept(clazz.getDeclaredConstructor().newInstance());
            }
        }

        Map<String, String> headers = EnvUtils.getClientProperties(key + ".headers", environment);
        channelBuilder.intercept(new HeaderClientInterceptor(headers));

        ManagedChannel channel = channelBuilder.build();

        // 2. Tìm outer class (GreetingServiceGrpc)
        Class<?> outerClass = stubClass.getEnclosingClass();
        if (outerClass == null) {
            throw new IllegalStateException("Stub class must be a nested class of the gRPC service class");
        }

        // 3. Gọi static method newBlockingStub(channel)
        Method newStubMethod = outerClass.getMethod("newBlockingStub", io.grpc.Channel.class);
        Object stubInstance = newStubMethod.invoke(null, channel);

        // 4. Tạo proxy client
        return GrpcClientProxyFactory.create(clientInterface, stubInstance);
    }

    @Override
    public Class<?> getObjectType() {
        return clientInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}