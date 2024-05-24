package org.example;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class HttpResponse {
    private final SocketChannel clientChannel;

    public HttpResponse(SocketChannel clientChannel) {
        this.clientChannel = clientChannel;
    }

    public void send(int statusCode, String body) throws IOException {
        send(statusCode, body, "text/plain");
    }

    public void send(int statusCode, String body, String contentType) throws IOException {
        String statusMessage = getStatusMessage(statusCode);
        String response = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "\r\n" +
                body;
        ByteBuffer buffer = ByteBuffer.wrap(response.getBytes());
        clientChannel.write(buffer);
        clientChannel.close();
    }

    private String getStatusMessage(int statusCode) {
        switch (statusCode) {
            case 200:
                return "OK";
            case 400:
                return "Bad Request";
            case 404:
                return "Not Found";
            case 500:
                return "Internal Server Error";
            default:
                return "Unknown Status";
        }
    }
}
