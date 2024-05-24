package org.example;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

public class HttpRequestHandler {
    private static final Map<String, JsonObject> dataStore = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public HttpRequestHandler() {
        // Инициализация примеров данных
        JsonObject exampleData = new JsonObject();
        exampleData.addProperty("field1", "value1");
        exampleData.addProperty("field2", "value2");
        dataStore.put("example", exampleData);
    }

    public void registerHandlers(HttpServer server) {
        // Добавляем обработчик для GET запроса на путь "/"
        server.addHandler("GET", "/", (request, response) -> {
            response.send(200, "Hello, World!");
        });

        // Добавляем обработчик для GET запроса на путь "/data" для вывода текущих данных
        server.addHandler("GET", "/data", (request, response) -> {
            StringBuilder data = new StringBuilder();
            dataStore.forEach((key, value) -> data.append(key).append(": ").append(value.toString()).append("\n"));
            response.send(200, data.toString());
        });

        // Добавляем обработчик для POST запроса на путь "/submit"
        server.addHandler("POST", "/submit", (request, response) -> {
            String requestBody = request.getBody();
            Map<String, String> newData = gson.fromJson(requestBody, new TypeToken<Map<String, String>>(){}.getType());
            if (newData != null && newData.containsKey("key") && newData.containsKey("value")) {
                JsonObject jsonObject = JsonParser.parseString(newData.get("value")).getAsJsonObject();
                dataStore.put(newData.get("key"), jsonObject);
                response.send(200, "New entry added: " + newData.get("key") + " = " + jsonObject.toString());
            } else {
                response.send(400, "Invalid data format. Expected JSON with 'key' and 'value'.");
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

        // Добавляем обработчик для POST запроса на путь "/upload" с поддержкой multipart/form-data
        server.addHandler("POST", "/upload", (request, response) -> {
            String contentType = request.getHeaders().get("Content-Type");
            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                InputStream inputStream = new ByteArrayInputStream(request.getBody().getBytes());
                MultipartParser parser = new MultipartParser(contentType, inputStream);
                List<Map<String, String>> parts = parser.parse();

                // Обработка частей multipart
                for (Map<String, String> part : parts) {
                    String disposition = part.get("Content-Disposition");
                    String filename = null;
                    if (disposition != null) {
                        String[] elements = disposition.split(";");
                        for (String element : elements) {
                            if (element.trim().startsWith("filename=")) {
                                filename = element.split("=")[1].replace("\"", "");
                            }
                        }
                    }
                    String fileContent = part.get("body");
                    System.out.println("Received file: " + (filename != null ? filename : "unknown"));
                    System.out.println("File content: " + fileContent);
                }

                response.send(200, "File uploaded successfully.");
            } else {
                response.send(400, "Bad Request: Expected multipart/form-data");
            }
        });
    }
}
