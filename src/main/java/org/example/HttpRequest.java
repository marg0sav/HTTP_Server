package org.example;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final String body;
    private final SocketChannel clientChannel;

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

    public Map<String, String> getQueryParams() {
        return parseParams(path.contains("?") ? path.split("\\?")[1] : "");
    }

    private Map<String, String> parseParams(String paramString) {
        Map<String, String> params = new HashMap<>();
        if (!paramString.isEmpty()) {
            String[] pairs = paramString.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }
}
