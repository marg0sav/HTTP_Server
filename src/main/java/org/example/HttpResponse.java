package org.example;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private final SocketChannel clientChannel;
    private boolean sent = false;

    public HttpResponse(SocketChannel clientChannel) {
        this.clientChannel = clientChannel;
    }

    public void send(int statusCode, String body) throws IOException {
        send(statusCode, body, "text/plain");
    }

    public void send(int statusCode, String body, String contentType) throws IOException {
        if (sent) {
            return; // Если ответ уже был отправлен, ничего не делать
        }
        String statusMessage = getStatusMessage(statusCode);
        String responseBody = (statusCode == 100) ? "" : statusCode + ": " + statusMessage + "\r\n" + body;
        String response = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Length: " + responseBody.length() + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "\r\n" +
                responseBody;
        ByteBuffer buffer = ByteBuffer.wrap(response.getBytes());
        clientChannel.write(buffer);

        // Закрываем канал только если статус не 100 "Continue"
        if (statusCode != 100) {
            sent = true;
            clientChannel.close();
            System.out.println("Client channel closed after sending response: " + statusCode);
        }
    }

    public void sendContinue() throws IOException {
        if (sent) {
            return; // Если ответ уже был отправлен, ничего не делать
        }
        String response = "HTTP/1.1 100 Continue\r\n\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(response.getBytes());
        clientChannel.write(buffer);
        System.out.println("Sent 100 Continue");
    }

    public boolean isSent() {
        return sent;
    }

    private String getStatusMessage(int statusCode) {
        Map<Integer, String> statusMessages = new HashMap<>();
        statusMessages.put(100, "Continue");
        statusMessages.put(101, "Switching Protocols");
        statusMessages.put(200, "OK");
        statusMessages.put(201, "Created");
        statusMessages.put(202, "Accepted");
        statusMessages.put(203, "Non-Authoritative Information");
        statusMessages.put(204, "No Content");
        statusMessages.put(205, "Reset Content");
        statusMessages.put(206, "Partial Content");
        statusMessages.put(300, "Multiple Choices");
        statusMessages.put(301, "Moved Permanently");
        statusMessages.put(302, "Found");
        statusMessages.put(303, "See Other");
        statusMessages.put(304, "Not Modified");
        statusMessages.put(305, "Use Proxy");
        statusMessages.put(307, "Temporary Redirect");
        statusMessages.put(400, "Bad Request");
        statusMessages.put(401, "Unauthorized");
        statusMessages.put(402, "Payment Required");
        statusMessages.put(403, "Forbidden");
        statusMessages.put(404, "Not Found");
        statusMessages.put(405, "Method Not Allowed");
        statusMessages.put(406, "Not Acceptable");
        statusMessages.put(407, "Proxy Authentication Required");
        statusMessages.put(408, "Request Timeout");
        statusMessages.put(409, "Conflict");
        statusMessages.put(410, "Gone");
        statusMessages.put(411, "Length Required");
        statusMessages.put(412, "Precondition Failed");
        statusMessages.put(413, "Payload Too Large");
        statusMessages.put(414, "URI Too Long");
        statusMessages.put(415, "Unsupported Media Type");
        statusMessages.put(416, "Range Not Satisfiable");
        statusMessages.put(417, "Expectation Failed");
        statusMessages.put(500, "Internal Server Error");
        statusMessages.put(501, "Not Implemented");
        statusMessages.put(502, "Bad Gateway");
        statusMessages.put(503, "Service Unavailable");
        statusMessages.put(504, "Gateway Timeout");
        statusMessages.put(505, "HTTP Version Not Supported");

        return statusMessages.getOrDefault(statusCode, "Unknown Status");
    }
}
