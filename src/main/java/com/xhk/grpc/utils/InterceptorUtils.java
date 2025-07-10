package com.xhk.grpc.utils;

import com.xhk.grpc.annotation.GrpcInterceptor;
import com.xhk.grpc.proxy.ConfigurableHeaderClientInterceptor;
import com.xhk.grpc.proxy.DeadlineInterceptor;
import com.xhk.grpc.proxy.DynamicHeaderClientInterceptor;
import com.xhk.grpc.proxy.RetryInterceptor;
import io.grpc.ClientInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Component
public class InterceptorUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        InterceptorUtils.applicationContext = applicationContext;
    }

    public static ClientInterceptor createInterceptor(GrpcInterceptor annotation) throws Exception {
        Class<? extends ClientInterceptor> interceptorClass = annotation.value();
        String[] args = annotation.args();
        String beanName = annotation.beanName();

        // Xử lý các interceptor đặc biệt
        if (interceptorClass == DeadlineInterceptor.class) {
            return createDeadlineInterceptor(args);
        } else if (interceptorClass == RetryInterceptor.class) {
            return createRetryInterceptor(args);
        } else if (interceptorClass == DynamicHeaderClientInterceptor.class) {
            return createDynamicHeaderInterceptor(beanName);
        } else if (interceptorClass == ConfigurableHeaderClientInterceptor.class) {
            return createConfigurableHeaderInterceptor(args);
        } else {
            // Xử lý các interceptor thông thường
            return createGenericInterceptor(interceptorClass, args);
        }
    }

    private static DeadlineInterceptor createDeadlineInterceptor(String[] args) {
        if (args.length == 0) {
            return new DeadlineInterceptor(Duration.ofSeconds(5)); // Default 5 seconds
        }

        String timeoutStr = args[0];
        Duration timeout;

        // Kiểm tra xem có phải là ISO-8601 duration format không
        if (timeoutStr.startsWith("P")) {
            timeout = Duration.parse(timeoutStr);
        } else {
            // Xử lý như milliseconds
            long millis = Long.parseLong(timeoutStr);
            timeout = Duration.ofMillis(millis);
        }

        return new DeadlineInterceptor(timeout);
    }

    private static RetryInterceptor createRetryInterceptor(String[] args) {
        if (args.length < 4) {
            throw new IllegalArgumentException("RetryInterceptor requires 4 arguments: maxAttempts, initialBackoffMillis, backoffMultiplier, retryableCodes");
        }

        int maxAttempts = Integer.parseInt(args[0]);
        long initialBackoffMillis = Long.parseLong(args[1]);
        double backoffMultiplier = Double.parseDouble(args[2]);
        Set<io.grpc.Status.Code> retryableCodes = parseRetryableCodes(args[3]);

        return new RetryInterceptor(maxAttempts, initialBackoffMillis, backoffMultiplier, retryableCodes);
    }

    private static Set<io.grpc.Status.Code> parseRetryableCodes(String codesStr) {
        Set<io.grpc.Status.Code> codes = new java.util.HashSet<>();
        String[] codeNames = codesStr.split(",");

        for (String codeName : codeNames) {
            try {
                codes.add(io.grpc.Status.Code.valueOf(codeName.trim()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status code: " + codeName);
            }
        }

        return codes;
    }

    private static DynamicHeaderClientInterceptor createDynamicHeaderInterceptor(String beanName) {
        if (beanName.isEmpty()) {
            throw new IllegalArgumentException("beanName is required for DynamicHeaderClientInterceptor");
        }

        Object bean = applicationContext.getBean(beanName);
        if (!(bean instanceof Supplier)) {
            throw new IllegalArgumentException("Bean " + beanName + " must implement Supplier<Map<String, String>>");
        }

        @SuppressWarnings("unchecked")
        Supplier<Map<String, String>> supplier = (Supplier<Map<String, String>>) bean;
        return new DynamicHeaderClientInterceptor(supplier);
    }

    private static ConfigurableHeaderClientInterceptor createConfigurableHeaderInterceptor(String[] args) {
        return new ConfigurableHeaderClientInterceptor(args);
    }

    private static ClientInterceptor createGenericInterceptor(Class<? extends ClientInterceptor> interceptorClass, String[] args) throws Exception {
        if (args.length == 0) {
            // Tìm constructor không tham số
            Constructor<? extends ClientInterceptor> constructor = interceptorClass.getDeclaredConstructor();
            return constructor.newInstance();
        } else {
            // Tìm constructor với tham số String[]
            Constructor<? extends ClientInterceptor> constructor = interceptorClass.getDeclaredConstructor(String[].class);
            return constructor.newInstance((Object) args);
        }
    }
} 