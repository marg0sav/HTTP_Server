package org.example;

import org.example.handlers.HttpRequestHandler;
import org.example.server.HttpServer;

import java.io.IOException;

public class ServerApp {
    public static void main(String[] args) {
        try {
            HttpServer server = new HttpServer("localhost", 8081);
            HttpRequestHandler handler = new HttpRequestHandler();
            handler.registerHandlers(server);

            // Setting a flag to simulate long-term operations
            HttpRequestHandler.setFlagForLongTimeout(true);

            // Setting the availability of the server
            handler.setServiceAvailable(true);

            // Setting the availability of the external server (if false, the connection
            // to a non-existent API)
            handler.setExternalServiceAvailable(true);

            // Adding shutdownhook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Server closed. Goodbye!");
            }));

            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
