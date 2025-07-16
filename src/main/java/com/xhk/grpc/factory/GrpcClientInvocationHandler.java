package com.xhk.grpc.factory;

import com.xhk.grpc.annotation.GrpcMethod;
import com.xhk.grpc.middleware.Middleware;
import com.xhk.grpc.middleware.MiddlewareChain;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public class GrpcClientInvocationHandler implements InvocationHandler {
    private final Object stub;
    private final List<Middleware> middlewares;

    public GrpcClientInvocationHandler(Object stub, List<Middleware> middlewares) {
        this.stub = Objects.requireNonNull(stub, "Stub instance must not be null");
        this.middlewares = middlewares != null ? middlewares : List.of();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        validateMethodArgs(method, args);

        String grpcMethodName = resolveGrpcMethodName(method);
        Object request = args[0];

        // Middleware chain
        MiddlewareChain<Object, Object> chain = new MiddlewareChain<>(middlewares, request);
        chain.execute();
        Object modifiedRequest = Objects.requireNonNull(chain.getRequest(), "Request after middleware chain is null");

        Method grpcMethod = findGrpcMethod(stub.getClass(), grpcMethodName, modifiedRequest);
        Object response = grpcMethod.invoke(stub, modifiedRequest);
        chain.setResponse(response);
        return response;
    }

    private void validateMethodArgs(Method method, Object[] args) {
        if (args == null || args.length != 1) {
            throw new IllegalArgumentException("Method '" + method.getName() + "' must have exactly one argument");
        }
        if (args[0] == null) {
            throw new IllegalArgumentException("Request argument must not be null");
        }
    }

    private String resolveGrpcMethodName(Method method) {
        GrpcMethod annotation = method.getAnnotation(GrpcMethod.class);
        return annotation != null ? annotation.value() : method.getName();
    }

    private Method findGrpcMethod(Class<?> stubClass, String methodName, Object param) throws NoSuchMethodException {
        for (Method m : stubClass.getMethods()) {
            if (m.getName().equals(methodName)
                    && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isAssignableFrom(param.getClass())) {
                return m;
            }
        }
        throw new NoSuchMethodException("gRPC stub has no method named '" + methodName +
                "' accepting parameter of type: " + param.getClass().getName());
    }
}