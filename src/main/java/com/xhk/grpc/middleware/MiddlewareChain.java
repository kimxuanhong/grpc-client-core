package com.xhk.grpc.middleware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MiddlewareChain<ReqT, RespT> implements MiddlewareContext<ReqT, RespT> {
    private final List<Middleware> middlewares = new ArrayList<>();
    private int index = 0;
    private ReqT request;
    private RespT response;
    private final Map<String, String> headers = new HashMap<>();
    private boolean stopped = false;

    public MiddlewareChain(List<Middleware> middlewares, ReqT request) {
        if (middlewares != null) this.middlewares.addAll(middlewares);
        this.request = request;
    }

    public void execute() {
        if (!middlewares.isEmpty()) {
            index = 0;
            next();
        }
    }

    @Override
    public ReqT getRequest() { return request; }

    @Override
    public void setRequest(ReqT request) { this.request = request; }

    @Override
    public RespT getResponse() { return response; }

    @Override
    public void setResponse(RespT response) { this.response = response; }

    @Override
    public Map<String, String> getHeaders() { return headers; }

    @Override
    public void setHeader(String key, String value) { headers.put(key, value); }

    @Override
    public void next() {
        if (stopped || index >= middlewares.size()) return;
        Middleware current = middlewares.get(index++);
        current.invoke(this);
    }

    @Override
    public boolean isChainStopped() { return stopped; }

    public void stopChain() { this.stopped = true; }
} 