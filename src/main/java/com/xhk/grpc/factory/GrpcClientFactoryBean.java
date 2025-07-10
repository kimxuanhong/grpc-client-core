package com.xhk.grpc.factory;

import com.xhk.grpc.annotation.GrpcInterceptor;
import com.xhk.grpc.proxy.DeadlineInterceptor;
import com.xhk.grpc.proxy.GrpcLogClientInterceptor;
import com.xhk.grpc.proxy.HeaderClientInterceptor;
import com.xhk.grpc.utils.EnvUtils;
import com.xhk.grpc.utils.InterceptorUtils;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;

public class GrpcClientFactoryBean<T> implements FactoryBean<T>, EnvironmentAware, ApplicationContextAware {
    private final Class<T> clientInterface;
    private final Class<?> stubClass;
    private Environment environment;
    private ApplicationContext applicationContext;
    private String url;
    private final Class<? extends ClientInterceptor>[] interceptorClasses;
    private final GrpcInterceptor[] interceptorConfigs;

    public GrpcClientFactoryBean(Class<T> clientInterface, Class<?> stubClass, Class<? extends ClientInterceptor>[] interceptorClasses) {
        this.clientInterface = clientInterface;
        this.stubClass = stubClass;
        this.interceptorClasses = interceptorClasses;
        this.interceptorConfigs = new GrpcInterceptor[0];
    }
    
    public GrpcClientFactoryBean(Class<T> clientInterface, Class<?> stubClass, GrpcInterceptor[] interceptorConfigs) {
        this.clientInterface = clientInterface;
        this.stubClass = stubClass;
        this.interceptorClasses = new Class[0];
        this.interceptorConfigs = interceptorConfigs;
    }
    
    public GrpcClientFactoryBean(Class<T> clientInterface, Class<?> stubClass, Class<? extends ClientInterceptor>[] interceptorClasses, GrpcInterceptor[] interceptorConfigs) {
        this.clientInterface = clientInterface;
        this.stubClass = stubClass;
        this.interceptorClasses = interceptorClasses;
        this.interceptorConfigs = interceptorConfigs;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
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
        
        // Xử lý interceptors từ config (cách mới)
        if (interceptorConfigs != null) {
            for (GrpcInterceptor config : interceptorConfigs) {
                ClientInterceptor interceptor = InterceptorUtils.createInterceptor(config);
                channelBuilder.intercept(interceptor);
            }
        }
        
        // Xử lý interceptors từ class array (cách cũ)
        if (interceptorClasses != null) {
            for (Class<? extends ClientInterceptor> clazz : interceptorClasses) {
                ClientInterceptor interceptor = createInterceptorFromClass(clazz);
                channelBuilder.intercept(interceptor);
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
    
    private ClientInterceptor createInterceptorFromClass(Class<? extends ClientInterceptor> clazz) throws Exception {
        // Thử lấy bean từ Spring context trước
        try {
            return applicationContext.getBean(clazz);
        } catch (Exception e) {
            // Nếu không có bean, tạo instance mới bằng constructor không tham số
            return clazz.getDeclaredConstructor().newInstance();
        }
    }
}