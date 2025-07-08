package com.xhk.grpc.factory;


import org.springframework.beans.factory.FactoryBean;

public class GrpcClientFactoryBean<T> implements FactoryBean<T> {
    private final Class<T> clientInterface;
    private final Class<?> stubClass;

    public GrpcClientFactoryBean(Class<T> clientInterface, Class<?> stubClass) {
        this.clientInterface = clientInterface;
        this.stubClass = stubClass;
    }

    @Override
    public T getObject() throws Exception {
        Object stub = stubClass.getDeclaredConstructor().newInstance();
        return GrpcClientProxyFactory.create(clientInterface, stub);
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