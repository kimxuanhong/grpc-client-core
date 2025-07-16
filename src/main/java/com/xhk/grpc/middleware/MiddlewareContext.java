package com.xhk.grpc.middleware;

import java.util.Map;

public interface MiddlewareContext<ReqT, RespT> {
    ReqT getRequest();
    void setRequest(ReqT request);
    RespT getResponse();
    void setResponse(RespT response);
    Map<String, String> getHeaders();
    void setHeader(String key, String value);
    void next(); // Gọi middleware tiếp theo trong chain
    boolean isChainStopped(); // Cho phép middleware dừng chain
} 