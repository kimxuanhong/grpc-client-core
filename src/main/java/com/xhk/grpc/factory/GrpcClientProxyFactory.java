package com.xhk.grpc.factory;

import com.xhk.grpc.annotation.GrpcMethod;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class GrpcClientProxyFactory {
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> grpcInterface, Object stub) {
        return (T) Proxy.newProxyInstance(
                grpcInterface.getClassLoader(),
                new Class[]{grpcInterface},
                (proxy, method, args) -> {
                    String grpcMethodName = method.isAnnotationPresent(GrpcMethod.class)
                            ? method.getAnnotation(GrpcMethod.class).value()
                            : method.getName();

                    if (args == null || args.length != 1) {
                        throw new IllegalArgumentException("gRPC client method must have exactly one argument (the request object)");
                    }
                    Object request = args[0];

                    Method grpcMethod = null;
                    for (Method m : stub.getClass().getMethods()) {
                        if (m.getName().equals(grpcMethodName) && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(request.getClass())) {
                            grpcMethod = m;
                            break;
                        }
                    }
                    if (grpcMethod == null) {
                        throw new NoSuchMethodException("No suitable gRPC stub method '" + grpcMethodName + "' accepting parameter of type " + request.getClass().getName());
                    }
                    return grpcMethod.invoke(stub, request);
                }
        );
    }
}