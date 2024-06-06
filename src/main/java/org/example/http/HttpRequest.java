package org.example.http;

import java.nio.channels.SocketChannel;
import java.util.Map;

/**
 * The HttpRequest class represents an HTTP request.
 * It contains the HTTP method, the request path, headers, body, and the client channel.
 */
public class HttpRequest {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final String body;
    private final SocketChannel clientChannel;

    /**
     * Constructs an HttpRequest with the specified method, path, headers, body, and client channel.
     *
     * @param method        the HTTP method (e.g., GET, POST, PUT, DELETE)
     * @param path          the request path
     * @param headers       the request headers
     * @param body          the request body
     * @param clientChannel the client channel
     */
    public HttpRequest(String method, String path, Map<String, String> headers, String body, SocketChannel clientChannel) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
        this.clientChannel = clientChannel;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public SocketChannel getClientChannel() {
        return clientChannel;
    }

}
