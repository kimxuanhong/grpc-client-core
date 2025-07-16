package com.xhk.grpc.factory;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.lang.reflect.Method;

public class StubCreator {
    public static Object create(Class<?> stubClass, String url) throws Exception {
        Class<?> outerClass = stubClass.getEnclosingClass();
        if (outerClass == null) {
            throw new IllegalStateException("Stub class must be a nested class of the gRPC service class");
        }

        Method newStubMethod = outerClass.getMethod("newBlockingStub", io.grpc.Channel.class);
        ManagedChannel channel = ManagedChannelBuilder.forTarget(url).usePlaintext().build();
        return newStubMethod.invoke(null, channel);
    }
}
