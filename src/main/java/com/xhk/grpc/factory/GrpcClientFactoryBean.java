package com.xhk.grpc.factory;

import com.xhk.grpc.middleware.Middleware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Objects;

public class GrpcClientFactoryBean<T> implements FactoryBean<T>, EnvironmentAware, ApplicationContextAware {
    private final Class<T> clientInterface;
    private final Class<?> stubClass;
    private final Class<? extends Middleware>[] middlewareClasses;

    private Environment environment;
    private ApplicationContext applicationContext;
    private String url;

    public GrpcClientFactoryBean(Class<T> clientInterface, Class<?> stubClass,
                                 Class<? extends Middleware>[] middlewareClasses) {
        this.clientInterface = Objects.requireNonNull(clientInterface);
        this.stubClass = Objects.requireNonNull(stubClass);
        this.middlewareClasses = middlewareClasses;
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
        GrpcClientConfig config = GrpcClientConfigLoader.load(clientInterface, environment, url);
        List<Middleware> middlewares = MiddlewareChainBuilder.build(middlewareClasses, applicationContext, config);
        Object stub = StubCreator.create(stubClass, config.getUrl());

        return GrpcClientProxyFactory.create(clientInterface, stub, middlewares);
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
