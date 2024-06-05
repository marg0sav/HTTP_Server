package org.example;

import java.io.IOException;

public class ServerApp {
    public static void main(String[] args) {
        try {
            HttpServer server = new HttpServer("localhost", 8081);
            HttpRequestHandler handler = new HttpRequestHandler();
            handler.registerHandlers(server);

            // Устанавливаем флаг для симуляции длительных операций
            HttpRequestHandler.setFlagForLongTimeout(false);

            // Устанавливаем доступность сервера
            handler.setServiceAvailable(true);

            // Устанавливаем доступность внешнего сервера (при false, происходит подключение к
            // несуществующему API)
            handler.setExternalServiceAvailable(true);

            // Добавляем shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Server closed. Goodbye!");
            }));

            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
