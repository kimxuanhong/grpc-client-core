package com.xhk.grpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcInterceptor {
    /**
     * Class của ClientInterceptor
     */
    Class<? extends io.grpc.ClientInterceptor> value();
    
    /**
     * Các tham số để khởi tạo interceptor
     * Các tham số sẽ được truyền vào constructor của interceptor
     */
    String[] args() default {};
    
    /**
     * Tên của bean Spring để inject (cho DynamicHeaderClientInterceptor)
     */
    String beanName() default "";
} 