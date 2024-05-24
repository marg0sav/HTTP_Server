package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class HttpServerChannel {
    private final String host;
    private final int port;
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final Map<String, Map<String, HttpHandler>> handlers;

    public HttpServerChannel(String host, int port, Map<String, Map<String, HttpHandler>> handlers) throws IOException {
        this.host = host;
        this.port = port;
        this.handlers = handlers;
        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.bind(new InetSocketAddress(host, port));
        this.serverChannel.configureBlocking(false);
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void start() throws IOException {
        System.out.println("Server started on " + host + ":" + port);

        while (true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                if (!key.isValid()) continue;

                if (key.isAcceptable()) {
                    accept(selector, key);
                } else if (key.isReadable()) {
                    read(key);
                }
            }
        }
    }

    private void accept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int read = clientChannel.read(buffer);

        if (read == -1) {
            clientChannel.close();
            key.cancel();
            return;
        }

        buffer.flip();
        String request = new String(buffer.array(), 0, read);
        handleRequest(clientChannel, request);
    }

    private void handleRequest(SocketChannel clientChannel, String request) throws IOException {
        String[] lines = request.split("\r\n");

        if (lines.length == 0 || lines[0].isEmpty()) {
            sendBadRequestResponse(clientChannel);
            return;
        }

        String[] requestLine = lines[0].split(" ");
        if (requestLine.length < 2) {
            sendBadRequestResponse(clientChannel);
            return;
        }

        String method = requestLine[0];
        String path = requestLine[1];

        Map<String, String> headers = new HashMap<>();
        int i = 1;
        while (i < lines.length && !lines[i].isEmpty()) {
            String[] header = lines[i].split(": ");
            if (header.length == 2) {
                headers.put(header[0], header[1]);
            }
            i++;
        }

        String body = "";
        if (i + 1 < lines.length) {
            body = lines[i + 1];
        }

        HttpRequest httpRequest = new HttpRequest(method, path, headers, body);
        HttpResponse httpResponse = new HttpResponse(clientChannel);

        if (handlers.containsKey(method) && handlers.get(method).containsKey(path)) {
            handlers.get(method).get(path).handle(httpRequest, httpResponse);
        } else {
            httpResponse.send(404, "Not Found");
        }
    }

    private void sendBadRequestResponse(SocketChannel clientChannel) throws IOException {
        HttpResponse httpResponse = new HttpResponse(clientChannel);
        httpResponse.send(400, "Bad Request");
    }
}
