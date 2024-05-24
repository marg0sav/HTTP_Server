package org.example;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpServer {
    private final Map<String, Map<String, HttpHandler>> handlers = new ConcurrentHashMap<>();
    private final String host;
    private final int port;
    private HttpServerChannel serverChannel;

    public HttpServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws IOException {
        serverChannel = new HttpServerChannel(host, port, handlers);
        serverChannel.start();
    }

    public void addHandler(String method, String path, HttpHandler handler) {
        handlers.putIfAbsent(method, new ConcurrentHashMap<>());
        handlers.get(method).put(path, handler);
    }
}