package com.xhk.grpc.annotation;

import com.xhk.grpc.middleware.Middleware;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcClient {
    String url() default "";

    Class<?> stub();

    Class<? extends Middleware>[] middlewares() default {};
}
