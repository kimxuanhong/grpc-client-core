package com.xhk.grpc.factory;

import com.xhk.grpc.middleware.Middleware;

import java.lang.reflect.Proxy;
import java.util.List;

public class GrpcClientProxyFactory {

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> grpcInterface, Object stub, List<Middleware> middlewares) {
        return (T) Proxy.newProxyInstance(
                grpcInterface.getClassLoader(),
                new Class[]{grpcInterface},
                new GrpcClientInvocationHandler(stub, middlewares)
        );
    }
}