package org.example.server;

import org.example.handlers.HttpHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The HttpServer class represents a simple HTTP server that handles incoming HTTP requests.
 * It maintains a map of HTTP handlers for different methods and paths.
 */
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

    public void stop() throws IOException {
        if (serverChannel != null) {
            serverChannel.stop();
        }
    }

    public void addHandler(String method, String path, HttpHandler handler) {
        handlers.putIfAbsent(method, new ConcurrentHashMap<>());
        handlers.get(method).put(path, handler);
    }
}
