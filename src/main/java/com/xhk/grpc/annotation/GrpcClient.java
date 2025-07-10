package com.xhk.grpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcClient {
    String url() default "";

    Class<?> stub();

    /**
     * Các interceptor class trực tiếp (cách cũ)
     */
    Class<? extends io.grpc.ClientInterceptor>[] interceptors() default {};
    
    /**
     * Các interceptor với cấu hình chi tiết (cách mới)
     */
    GrpcInterceptor[] interceptorsConfig() default {};
}
