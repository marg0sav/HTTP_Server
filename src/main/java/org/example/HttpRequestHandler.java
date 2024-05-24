package org.example;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.net.HttpURLConnection;
import java.net.URL;



public class HttpRequestHandler {
    private static final Map<String, JsonObject> dataStore = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private static final String UPLOAD_DIR = "uploads"; // Директория для сохранения файлов
    private final boolean delayEnabled = false; // Флаг для добавления задержки
    private boolean serviceAvailable; // Флаг для проверки доступности сервиса
    private boolean externalServiceAvailable = true; // Флаг для проверки доступности внешнего ресурса


    public HttpRequestHandler() {
        // Инициализация примеров данных
        JsonObject exampleData = new JsonObject();
        exampleData.addProperty("field1", "value1");
        exampleData.addProperty("field2", "value2");
        dataStore.put("example", exampleData);

        // Создание директории для загрузок, если она не существует
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
                System.out.println("Created upload directory: " + UPLOAD_DIR);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void registerHandlers(HttpServer server) {
        // Добавляем обработчик для GET запроса на путь "/"
        server.addHandler("GET", "/", (request, response) -> {
            response.send(200, "Hello, World!");
        });

        // Добавляем обработчик для проверки статуса 100 "Continue"
        server.addHandler("POST", "/continue", this::handleContinueRequest);

        // Добавляем обработчик для проверки статуса 502
        server.addHandler("GET", "/external", this::handleExternalRequest);

        // Добавляем обработчик для GET запроса на путь "/data" для вывода текущих данных
        server.addHandler("GET", "/data", this::handleDataRequest);

        // Добавляем обработчик для POST запроса на путь "/submit"
        server.addHandler("POST", "/submit", (request, response) -> {
            String requestBody = request.getBody();
            Map<String, String> newData = gson.fromJson(requestBody, new TypeToken<Map<String, String>>(){}.getType());
            if (newData != null && newData.containsKey("key") && newData.containsKey("value")) {
                JsonObject jsonObject = JsonParser.parseString(newData.get("value")).getAsJsonObject();
                dataStore.put(newData.get("key"), jsonObject);
                response.send(201, "New entry added: " + newData.get("key") + " = " + jsonObject.toString());
            } else {
                response.send(400, "Invalid data format. Expected JSON with 'key' and 'value'.");
            }
        });

        server.addHandler("POST", "/expect", (request, response) -> {
            String expectHeader = request.getHeaders().get("Expect");
            if (expectHeader != null && expectHeader.equalsIgnoreCase("100-continue")) {
                response.send(100, "Continue");
            } else {
                response.send(417, "Expectation Failed");
            }
        });

        // Добавляем обработчик для PUT запроса на путь "/update"
        server.addHandler("PUT", "/update", (request, response) -> {
            String requestBody = request.getBody();
            Map<String, String> updatedData = gson.fromJson(requestBody, new TypeToken<Map<String, String>>(){}.getType());
            if (updatedData != null && updatedData.containsKey("key") && updatedData.containsKey("value")) {
                if (dataStore.containsKey(updatedData.get("key"))) {
                    JsonObject jsonObject = JsonParser.parseString(updatedData.get("value")).getAsJsonObject();
                    dataStore.put(updatedData.get("key"), jsonObject);
                    response.send(200, "Updated entry: " + updatedData.get("key") + " = " + jsonObject.toString());
                } else {
                    response.send(404, "Data not found for key: " + updatedData.get("key"));
                }
            } else {
                response.send(400, "Invalid data format. Expected JSON with 'key' and 'value'.");
            }
        });

        // Добавляем обработчик для PATCH запроса на путь "/modify"
        server.addHandler("PATCH", "/modify", (request, response) -> {
            String requestBody = request.getBody();
            Map<String, String> modifiedData = gson.fromJson(requestBody, new TypeToken<Map<String, String>>(){}.getType());
            if (modifiedData != null && modifiedData.containsKey("key") && modifiedData.containsKey("value")) {
                if (dataStore.containsKey(modifiedData.get("key"))) {
                    JsonObject existingObject = dataStore.get(modifiedData.get("key"));
                    JsonObject modifications = JsonParser.parseString(modifiedData.get("value")).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : modifications.entrySet()) {
                        existingObject.add(entry.getKey(), entry.getValue());
                    }
                    dataStore.put(modifiedData.get("key"), existingObject);
                    response.send(200, "Modified entry: " + modifiedData.get("key") + " = " + existingObject.toString());
                } else {
                    response.send(404, "Data not found for key: " + modifiedData.get("key"));
                }
            } else {
                response.send(400, "Invalid data format. Expected JSON with 'key' and 'value'.");
            }
        });

        // Добавляем обработчик для DELETE запроса на путь "/delete"
        server.addHandler("DELETE", "/delete", (request, response) -> {
            String requestBody = request.getBody();
            Map<String, String> deleteData = gson.fromJson(requestBody, new TypeToken<Map<String, String>>(){}.getType());
            if (deleteData != null && deleteData.containsKey("key")) {
                if (dataStore.remove(deleteData.get("key")) != null) {
                    response.send(200, "Deleted entry with key: " + deleteData.get("key"));
                } else {
                    response.send(404, "Data not found for key: " + deleteData.get("key"));
                }
            } else {
                response.send(400, "Invalid data format. Expected JSON with 'key'.");
            }
        });

        // Обработчик для POST запроса на путь "/upload" с поддержкой multipart/form-data
        server.addHandler("POST", "/upload", (request, response) -> {
            String contentType = request.getHeaders().get("Content-Type");
            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                InputStream inputStream = new ByteArrayInputStream(request.getBody().getBytes());
                MultipartParser parser = new MultipartParser(contentType, inputStream);
                List<Map<String, String>> parts = null;
                try {
                    parts = parser.parse();
                } catch (IOException e) {
                    e.printStackTrace();
                    response.send(500, "Internal Server Error: Unable to parse multipart data");
                    return;
                }

                // Обработка частей multipart
                for (Map<String, String> part : parts) {
                    String disposition = part.get("Content-Disposition");
                    String filename = null;
                    if (disposition != null) {
                        String[] elements = disposition.split(";");
                        for (String element : elements) {
                            System.out.println("Disposition element: " + element);
                            if (element.trim().startsWith("filename=")) {
                                filename = element.split("=")[1].replace("\"", "");
                                System.out.println("Parsed filename: " + filename);
                            }
                        }
                    }
                    String fileContent = part.get("body");
                    if (filename != null) {
                        System.out.println("Received file: " + filename);
                        System.out.println("File content: " + fileContent);
                        // Сохранение файла
                        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(UPLOAD_DIR, filename))) {
                            writer.write(fileContent);
                            System.out.println("File saved to: " + Paths.get(UPLOAD_DIR, filename).toAbsolutePath().toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                            response.send(500, "Internal Server Error: Unable to save file");
                            return;
                        }
                    } else {
                        System.out.println("Filename is null");
                    }
                }

                response.send(200, "File uploaded successfully.");
            } else {
                response.send(400, "Bad Request: Expected multipart/form-data");
            }
        });

        // Добавляем обработчики для проверки различных статусов
        server.addHandler("GET", "/status/200", (request, response) -> {
            response.send(200, "OK");
        });

        server.addHandler("GET", "/status/201", (request, response) -> {
            response.send(201, "Created");
        });

        server.addHandler("GET", "/status/400", (request, response) -> {
            response.send(400, "Bad Request");
        });

        server.addHandler("GET", "/status/401", (request, response) -> {
            response.send(401, "Unauthorized");
        });

        server.addHandler("GET", "/status/403", (request, response) -> {
            response.send(403, "Forbidden");
        });

        server.addHandler("GET", "/status/404", (request, response) -> {
            response.send(404, "Not Found");
        });

        server.addHandler("GET", "/status/500", (request, response) -> {
            response.send(500, "Internal Server Error");
        });

        server.addHandler("GET", "/status/503", (request, response) -> {
            response.send(503, "Service Unavailable");
        });
    }

    private void handleDataRequest(HttpRequest request, HttpResponse response) throws IOException {
        try {
            if (isServiceUnavailable()) {
                response.send(503, "Service Unavailable");
                return;
            }
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                if (delayEnabled) { //Проверяем флаг для проверки статуса 504
                    // Если флаг установлен, выполняем длительную операцию по получению данных
                    processDataRequest();
                }
            });

            // Устанавливаем тайм-аут на 5 секунд для выполнения операции
            future.get(5, TimeUnit.SECONDS);

            // Если операция завершается успешно, отправляем данные
            StringBuilder data = new StringBuilder();
            dataStore.forEach((key, value) -> data.append(key).append(": ").append(value.toString()).append("\n"));
            response.send(200, data.toString());
        } catch (TimeoutException e) {
            // Если операция занимает слишком много времени, возвращаем статус 504
            response.send(504, "Gateway Time-out");
        } catch (Exception e) {
            // Обрабатываем другие исключения и возвращаем статус 500
            response.send(500, "Internal Server Error");
        }
    }

    private boolean isServiceUnavailable() {
        // Возвращает true, если сервис недоступен, иначе false
        return !serviceAvailable;
    }

    // Метод для установки доступности сервиса
    public void setServiceAvailable(boolean available) {
        this.serviceAvailable = available;
    }

    private void processDataRequest() {
        // Симулируем длительную операцию
        try {
            Thread.sleep(10000); // Задержка в 10 секунд
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleExternalRequest(HttpRequest request, HttpResponse response) throws IOException {
        try {
            if (isExternalServiceUnavailable()) {
                response.send(502, "Bad Gateway");
                return;
            }

            // Пример обращения к внешнему API
            URL url = new URL("https://jsonplaceholder.typicode.com/posts/1");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                // Закрываем потоки
                in.close();
                conn.disconnect();
                response.send(200, content.toString());
            } else {
                response.send(502, "Bad Gateway");
            }
        } catch (Exception e) {
            response.send(502, "Bad Gateway");
        }
    }


    private boolean isExternalServiceUnavailable() {
        // Возвращает true, если внешний сервис недоступен, иначе false
        return !externalServiceAvailable;
    }

    // Метод для установки доступности внешнего сервиса
    public void setExternalServiceAvailable(boolean available) {
        this.externalServiceAvailable = available;
    }

    private void handleContinueRequest(HttpRequest request, HttpResponse response) throws IOException {
        String expectHeader = request.getHeaders().get("Expect");
        if (expectHeader != null && expectHeader.equalsIgnoreCase("100-continue")) {
            // Отправляем статус 100 "Continue"
            response.sendContinue();
            System.out.println("Sent 100 Continue");

            // Чтение тела запроса после отправки 100 "Continue"
            String requestBody = readRequestBody(request.getClientChannel());
            System.out.println("Request Body: " + requestBody);

            // Обработка тела запроса
            response.send(200, "Received data: " + requestBody);
        } else {
            response.send(417, "Expectation Failed");
        }
    }

    private String readRequestBody(SocketChannel clientChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        StringBuilder requestBody = new StringBuilder();
        Selector selector = Selector.open();
        clientChannel.register(selector, SelectionKey.OP_READ);

        boolean reading = true;
        while (reading) {
            selector.select(500);  // тайм-аут 500 мс для избежания бесконечного ожидания
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isReadable()) {
                    int bytesRead = clientChannel.read(buffer);
                    if (bytesRead > 0) {
                        buffer.flip();
                        byte[] bytes = new byte[buffer.limit()];
                        buffer.get(bytes);
                        requestBody.append(new String(bytes));
                        buffer.clear();
                    } else if (bytesRead == -1) {
                        reading = false; // конец потока, выходим из цикла
                    }
                }
            }
            if (selectedKeys.isEmpty()) {
                reading = false; // нет больше данных, выходим из цикла
            }
        }
        return requestBody.toString();
    }
}