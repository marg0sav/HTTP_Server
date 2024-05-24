package org.example;

import java.io.IOException;

public class ServerApp {
    public static void main(String[] args) throws IOException {
        // Создаем экземпляр HTTP сервера
        HttpServer server = new HttpServer("localhost", 8081);

        // Создаем экземпляр обработчика запросов
        HttpRequestHandler requestHandler = new HttpRequestHandler();

        // Регистрация обработчиков запросов
        requestHandler.registerHandlers(server);

        // Запускаем сервер
        server.start();
    }
}
