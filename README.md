# grpc-client-core

## Giới thiệu

`grpc-client-core` là thư viện hỗ trợ tự động tạo và quản lý các gRPC client trong ứng dụng Spring Boot thông qua annotation, giúp việc gọi gRPC service trở nên đơn giản, dễ mở rộng và dễ kiểm soát.

## Cách hoạt động

- Sử dụng annotation `@EnableGrpcClients` để tự động quét và đăng ký các interface client có đánh dấu `@GrpcClient`.
- Mỗi interface client sẽ được proxy hóa, ánh xạ các method sang các method tương ứng của gRPC stub dựa trên annotation `@GrpcMethod` hoặc tên method.
- Hỗ trợ cấu hình endpoint động qua properties hoặc trực tiếp trên annotation.
- Hỗ trợ interceptor cho từng client.

## Hướng dẫn sử dụng

### 1. Thêm annotation vào cấu hình Spring Boot
```java
import com.xhk.grpc.config.EnableGrpcClients;

@EnableGrpcClients(basePackages = "com.example.grpcclient")
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 2. Định nghĩa interface client
```java
import com.xhk.grpc.annotation.GrpcClient;
import com.xhk.grpc.annotation.GrpcMethod;
import com.example.helloworld.GreeterGrpc;
import com.example.helloworld.HelloRequest;
import com.example.helloworld.HelloReply;

@GrpcClient(stub = GreeterGrpc.GreeterBlockingStub.class, url = "localhost:50051")
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```
- `stub`: class stub được generate từ proto (thường là `xxxGrpc.xxxBlockingStub.class`).
- `url`: endpoint của service (có thể bỏ qua để cấu hình qua properties).

### 3. Sử dụng client trong service
```java
@Service
public class MyService {
    @Autowired
    private GreeterClient greeterClient;

    public String hello(String name) {
        HelloRequest req = HelloRequest.newBuilder().setName(name).build();
        HelloReply reply = greeterClient.sayHello(req);
        return reply.getMessage();
    }
}
```

### 4. Cấu hình endpoint qua properties (tùy chọn)
```properties
grpc.clients.GreeterClient.url=localhost:50051
```

## Giải thích annotation

- `@EnableGrpcClients(basePackages = {...})`: Kích hoạt auto scan các interface client.
- `@GrpcClient(stub, url, interceptors)`: Đánh dấu interface là gRPC client, chỉ định stub, endpoint và interceptor nếu có.
- `@GrpcMethod(value)`: Đánh dấu method, ánh xạ tới method tương ứng của stub (nếu không khai báo sẽ lấy theo tên method).

## Cơ chế hoạt động nội bộ
- Khi ứng dụng khởi động, `GrpcClientRegistrar` sẽ scan các interface có `@GrpcClient` và đăng ký bean proxy.
- `GrpcClientFactoryBean` tạo channel, stub và proxy client.
- `GrpcClientProxyFactory` ánh xạ method interface sang method stub thực tế.

## Tuỳ chỉnh nâng cao

### Cách sử dụng Interceptor

Có 2 cách để sử dụng interceptor:

#### 1. Cách cũ (interceptors) - Hỗ trợ Spring DI
```java
@GrpcClient(
    stub = GreeterGrpc.GreeterBlockingStub.class, 
    url = "localhost:50051",
    interceptors = {CustomAuthInterceptor.class} // Sẽ được inject từ Spring context
)
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```

**Ưu điểm:**
- Hỗ trợ Spring Dependency Injection
- Có thể inject beans vào interceptor
- Đơn giản, chỉ cần chỉ định class

**Nhược điểm:**
- Không thể truyền tham số cho constructor
- Phải tạo interceptor như Spring bean

#### 2. Cách mới (interceptorsConfig) - Hỗ trợ tham số
```java
@GrpcClient(
    stub = GreeterGrpc.GreeterBlockingStub.class, 
    url = "localhost:50051",
    interceptorsConfig = {
        @GrpcInterceptor(DeadlineInterceptor.class, args = {"PT10S"}),
        @GrpcInterceptor(ConfigurableHeaderClientInterceptor.class, args = {
            "Authorization=Bearer token123"
        })
    }
)
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```

**Ưu điểm:**
- Có thể truyền tham số cho constructor
- Cấu hình linh hoạt
- Hỗ trợ beanName cho DynamicHeaderClientInterceptor

**Nhược điểm:**
- Không hỗ trợ Spring DI trực tiếp
- Phức tạp hơn một chút

#### 3. Sử dụng cùng lúc cả hai cách

Bạn có thể kết hợp cả hai cách để tận dụng ưu điểm của mỗi cách:

```java
@GrpcClient(
    stub = GreeterGrpc.GreeterBlockingStub.class, 
    url = "localhost:50051",
    interceptors = {CustomAuthInterceptor.class}, // Cách cũ - Spring DI
    interceptorsConfig = {
        @GrpcInterceptor(DeadlineInterceptor.class, args = {"PT10S"}), // Cách mới - tham số
        @GrpcInterceptor(ConfigurableHeaderClientInterceptor.class, args = {
            "X-Client-Version=1.0.0",
            "X-Request-ID=static-id"
        })
    }
)
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```

**Ưu điểm:**
- Tận dụng được cả Spring DI và tham số
- Linh hoạt tối đa trong việc cấu hình
- Có thể sử dụng interceptor có DI cho logic phức tạp và interceptor có tham số cho cấu hình đơn giản

## Sử dụng ClientInterceptors

Thư viện cung cấp sẵn các ClientInterceptor để xử lý các tác vụ phổ biến như logging, retry, timeout, và header management.

### 1. DeadlineInterceptor - Cấu hình timeout

Đặt timeout cho các gRPC call:

```java
import com.xhk.grpc.proxy.DeadlineInterceptor;
import java.time.Duration;

@GrpcClient(
    stub = GreeterGrpc.GreeterBlockingStub.class, 
    url = "localhost:50051",
    interceptorsConfig = {
        @GrpcInterceptor(DeadlineInterceptor.class, args = {"5000"}) // 5 giây timeout
    }
)
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```

Hoặc sử dụng Duration:

```java
@GrpcClient(
    stub = GreeterGrpc.GreeterBlockingStub.class, 
    url = "localhost:50051",
    interceptorsConfig = {
        @GrpcInterceptor(DeadlineInterceptor.class, args = {"PT10S"}) // 10 giây timeout
    }
)
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```

### 2. ConfigurableHeaderClientInterceptor - Thêm header tĩnh

Thêm các header cố định vào mọi request:

```java
import com.xhk.grpc.proxy.ConfigurableHeaderClientInterceptor;

@GrpcClient(
    stub = GreeterGrpc.GreeterBlockingStub.class, 
    url = "localhost:50051",
    interceptorsConfig = {
        @GrpcInterceptor(ConfigurableHeaderClientInterceptor.class, args = {
            "Authorization=Bearer token123",
            "X-Client-Version=1.0.0",
            "X-Request-ID=static-id"
        })
    }
)
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```

### 3. DynamicHeaderClientInterceptor - Thêm header động

Thêm header được tạo động trong runtime:

```java
import com.xhk.grpc.proxy.DynamicHeaderClientInterceptor;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Component
public class DynamicHeaderSupplier {
    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + getCurrentToken());
        headers.put("X-Request-ID", generateRequestId());
        headers.put("X-User-ID", getCurrentUserId());
        return headers;
    }
    
    private String getCurrentToken() {
        // Logic lấy token hiện tại
        return "dynamic-token";
    }
    
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
    
    private String getCurrentUserId() {
        // Logic lấy user ID hiện tại
        return "user123";
    }
}

@GrpcClient(
    stub = GreeterGrpc.GreeterBlockingStub.class, 
    url = "localhost:50051",
    interceptorsConfig = {
        @GrpcInterceptor(DynamicHeaderClientInterceptor.class, 
            beanName = "dynamicHeaderSupplier")
    }
)
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```

### 4. GrpcLogClientInterceptor - Logging tự động

Tự động log request, response và status của các gRPC call:

```java
import com.xhk.grpc.proxy.GrpcLogClientInterceptor;

@GrpcClient(
    stub = GreeterGrpc.GreeterBlockingStub.class, 
    url = "localhost:50051",
    interceptorsConfig = {
        @GrpcInterceptor(GrpcLogClientInterceptor.class)
    }
)
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```

Interceptor này sẽ log:
- Method name được gọi
- Request payload (dạng JSON)
- Response payload (dạng JSON)
- Status code khi call kết thúc

### 5. RetryInterceptor - Tự động retry

Tự động retry khi gặp lỗi có thể retry được:

```java
import com.xhk.grpc.proxy.RetryInterceptor;
import io.grpc.Status;

@GrpcClient(
    stub = GreeterGrpc.GreeterBlockingStub.class, 
    url = "localhost:50051",
    interceptorsConfig = {
        @GrpcInterceptor(RetryInterceptor.class, args = {
            "3",           // maxAttempts: tối đa 3 lần thử
            "1000",        // initialBackoffMillis: delay ban đầu 1 giây
            "2.0",         // backoffMultiplier: nhân đôi delay mỗi lần retry
            "UNAVAILABLE,DEADLINE_EXCEEDED" // retryableCodes
        })
    }
)
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```

### 6. Kết hợp nhiều Interceptor

Có thể kết hợp nhiều interceptor cho một client:

#### Cách 1: Chỉ sử dụng interceptorsConfig
```java
@GrpcClient(
    stub = GreeterGrpc.GreeterBlockingStub.class, 
    url = "localhost:50051",
    interceptorsConfig = {
        @GrpcInterceptor(GrpcLogClientInterceptor.class),
        @GrpcInterceptor(DeadlineInterceptor.class, args = {"10000"}),
        @GrpcInterceptor(ConfigurableHeaderClientInterceptor.class, args = {
            "Authorization=Bearer token123"
        }),
        @GrpcInterceptor(RetryInterceptor.class, args = {
            "3", "1000", "2.0", "UNAVAILABLE,DEADLINE_EXCEEDED"
        })
    }
)
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```

#### Cách 2: Kết hợp cả hai cách (khuyến nghị)
```java
@GrpcClient(
    stub = GreeterGrpc.GreeterBlockingStub.class, 
    url = "localhost:50051",
    interceptors = {CustomAuthInterceptor.class}, // Spring DI cho logic phức tạp
    interceptorsConfig = {
        @GrpcInterceptor(GrpcLogClientInterceptor.class), // Logging
        @GrpcInterceptor(DeadlineInterceptor.class, args = {"10000"}), // Timeout
        @GrpcInterceptor(ConfigurableHeaderClientInterceptor.class, args = {
            "X-Client-Version=1.0.0",
            "X-Request-ID=static-id"
        }), // Headers tĩnh
        @GrpcInterceptor(RetryInterceptor.class, args = {
            "3", "1000", "2.0", "UNAVAILABLE,DEADLINE_EXCEEDED"
        }) // Retry
    }
)
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```

**Thứ tự thực thi interceptors:**
1. DeadlineInterceptor (từ properties)
2. GrpcLogClientInterceptor (từ properties)
3. CustomAuthInterceptor (cách cũ - Spring DI)
4. GrpcLogClientInterceptor (cách mới)
5. DeadlineInterceptor (cách mới)
6. ConfigurableHeaderClientInterceptor (cách mới)
7. RetryInterceptor (cách mới)
8. HeaderClientInterceptor (từ properties)

### 7. Tạo Custom Interceptor

Bạn có thể tạo interceptor tùy chỉnh bằng cách implement `ClientInterceptor`. Có 2 cách:

#### Cách 1: Interceptor với Spring DI (sử dụng cách cũ)

```java
import io.grpc.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthInterceptor implements ClientInterceptor {
    
    @Autowired
    private AuthService authService; // Inject Spring bean
    
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
            
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // Sử dụng injected bean
                String token = authService.getCurrentToken();
                headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + token);
                super.start(responseListener, headers);
            }
        };
    }
}

@GrpcClient(
    stub = GreeterGrpc.GreeterBlockingStub.class, 
    url = "localhost:50051",
    interceptors = {CustomAuthInterceptor.class} // Sử dụng cách cũ
)
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```

#### Cách 2: Interceptor với tham số (sử dụng cách mới)

```java
import io.grpc.*;

public class SimpleCustomInterceptor implements ClientInterceptor {
    
    private final String customValue;
    
    public SimpleCustomInterceptor(String customValue) {
        this.customValue = customValue;
    }
    
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
            
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(Metadata.Key.of("custom-header", Metadata.ASCII_STRING_MARSHALLER), customValue);
                super.start(responseListener, headers);
            }
        };
    }
}

@GrpcClient(
    stub = GreeterGrpc.GreeterBlockingStub.class, 
    url = "localhost:50051",
    interceptorsConfig = {
        @GrpcInterceptor(SimpleCustomInterceptor.class, args = {"my-custom-value"})
    }
)
public interface GreeterClient {
    @GrpcMethod("sayHello")
    HelloReply sayHello(HelloRequest request);
}
```

## Đóng góp
PR, issue và góp ý luôn được chào đón!
