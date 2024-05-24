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

        requestHandler.setServiceAvailable(true); // Установить сервис как доступный/недоступный

        requestHandler.setExternalServiceAvailable(false); // Установить внешний сервис как доступный/недоступный

        // Запускаем сервер
        server.start();
    }
}