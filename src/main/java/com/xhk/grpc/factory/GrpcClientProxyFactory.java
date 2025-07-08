package com.xhk.grpc.factory;


import com.xhk.grpc.annotation.GrpcMethod;
import com.xhk.grpc.annotation.GrpcParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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

                    String requestClassName = stub.getClass().getPackageName() + "." + capitalize(grpcMethodName) + "Request";
                    Class<?> reqClass = Class.forName(requestClassName);

                    Object request;
                    if (args.length == 1 && reqClass.isAssignableFrom(args[0].getClass())) {
                        request = args[0];
                    } else {
                        Object builder = reqClass.getMethod("newBuilder").invoke(null);
                        Parameter[] parameters = method.getParameters();

                        for (int i = 0; i < parameters.length; i++) {
                            for (Annotation annotation : parameters[i].getAnnotations()) {
                                if (annotation instanceof GrpcParam grpcParam) {
                                    String field = grpcParam.value();
                                    Method setter = findSetter(builder.getClass(), field, args[i]);
                                    setter.invoke(builder, args[i]);
                                }
                            }
                        }
                        request = builder.getClass().getMethod("build").invoke(builder);
                    }

                    Method grpcMethod = stub.getClass().getMethod(grpcMethodName, request.getClass());
                    return grpcMethod.invoke(stub, request);
                }
        );
    }

    private static Method findSetter(Class<?> builderClass, String fieldName, Object value) throws Exception {
        String setterName = "set" + capitalize(fieldName);
        for (Method method : builderClass.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                if (value == null || method.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
                    return method;
                }
            }
        }
        throw new NoSuchMethodException("No suitable setter for field: " + fieldName);
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}