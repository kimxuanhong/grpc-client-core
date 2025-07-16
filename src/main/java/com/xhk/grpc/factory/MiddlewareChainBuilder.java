package com.xhk.grpc.factory;

import com.xhk.grpc.middleware.Middleware;
import com.xhk.grpc.middleware.impl.DeadlineMiddleware;
import com.xhk.grpc.middleware.impl.HeaderMiddleware;
import com.xhk.grpc.middleware.impl.LogMiddleware;
import org.springframework.context.ApplicationContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MiddlewareChainBuilder {
    public static List<Middleware> build(Class<? extends Middleware>[] middlewareClasses,
                                         ApplicationContext ctx,
                                         GrpcClientConfig config) throws Exception {
        List<Middleware> chain = new ArrayList<>();

        if (middlewareClasses != null) {
            for (Class<? extends Middleware> clazz : middlewareClasses) {
                Middleware instance = resolveMiddleware(clazz, ctx);
                if (instance != null) chain.add(instance);
            }
        }

        chain.add(new DeadlineMiddleware(Duration.ofMillis(config.getTimeoutMs())));
        chain.add(new LogMiddleware());

        if (!config.getHeaders().isEmpty()) {
            chain.add(new HeaderMiddleware(config.getHeaders()));
        }

        return chain;
    }

    private static Middleware resolveMiddleware(Class<? extends Middleware> clazz,
                                                ApplicationContext ctx) throws Exception {
        try {
            return ctx.getBean(clazz);
        } catch (Exception e) {
            return clazz.getDeclaredConstructor().newInstance();
        }
    }
}
