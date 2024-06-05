package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;

public class HttpRequestHandler {
    private static final Map<String, JsonObject> dataStore = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private static boolean flagForLongTimeout = false;
    private static boolean flagForServiceAvailable = true;
    private static boolean flagForExternalServiceAvailable = true;

    public HttpRequestHandler() {
        JsonObject exampleData = new JsonObject();
        exampleData.addProperty("field1", "value1");
        exampleData.addProperty("field2", "value2");
        dataStore.put("example", exampleData);
    }

    public static void setFlagForLongTimeout(boolean flag) {
        flagForLongTimeout = flag;
    }

    public void setServiceAvailable(boolean available) {
        flagForServiceAvailable = available;
    }

    public void setExternalServiceAvailable(boolean available) {
        flagForExternalServiceAvailable = available;
    }

    public void registerHandlers(HttpServer server) {
        server.addHandler("GET", "/", this::handleRequestWithTimeout);
        server.addHandler("GET", "/data", this::handleRequestWithTimeout);
        server.addHandler("POST", "/submit", this::handleRequestWithTimeout);
        server.addHandler("PUT", "/update", this::handleRequestWithTimeout);
        server.addHandler("PATCH", "/modify", this::handleRequestWithTimeout);
        server.addHandler("DELETE", "/delete", this::handleRequestWithTimeout);
        server.addHandler("GET", "/external", this::handleRequestWithTimeout);
        server.addHandler("GET", "/secure/admin", this::handleSecureAdminRequest);
        server.addHandler("GET", "/secure/user", this::handleSecureUserRequest);
        server.addHandler("POST", "/register", this::handleRegisterRequest);
        server.addHandler("POST", "/login", this::handleLoginRequest);
        server.addHandler("POST", "/continue", this::handleContinueRequest); // Добавить обработчик для проверки статуса 100 "Continue"
        server.addHandler("GET", "/redirect", this::handleRedirect);
    }

    private void handleRequestWithTimeout(HttpRequest request, HttpResponse response) {
        if (!flagForServiceAvailable) {
            try {
                response.send(503, "Service Unavailable");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (request.getBody().length() > 1024 * 1024) { // Ограничение в 1 МБ
            try {
                response.send(413, "Payload Too Large");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        Future<?> future = executorService.submit(() -> {
            try {
                handleRequest(request, response);
            } catch (IOException e) {
                if (!response.isSent()) {
                    try {
                        response.send(500, "Internal Server Error");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            if (!response.isSent()) {
                try {
                    response.send(504, "Gateway Timeout");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            future.cancel(true);
        } catch (Exception e) {
            if (!response.isSent()) {
                try {
                    response.send(500, "Internal Server Error");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void handleRequest(HttpRequest request, HttpResponse response) throws IOException {
        switch (request.getMethod()) {
            case "GET":
                if ("/".equals(request.getPath())) {
                    response.send(200, "Hello, World!");
                } else if ("/data".equals(request.getPath())) {
                    try {
                        // Добавить задержку в 5 секунд
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    response.send(200, gson.toJson(dataStore), "application/json");
                } else if ("/secure/user".equals(request.getPath())) {
                    handleSecureUserRequest(request, response);
                } else if ("/secure/admin".equals(request.getPath())) {
                    handleSecureAdminRequest(request, response);
                } else {
                    response.send(404, "Not Found");
                }
                break;
            case "POST":
                if ("/submit".equals(request.getPath())) {
                    handlePostSubmit(request, response);
                } else if ("/register".equals(request.getPath())) {
                    handleRegisterRequest(request, response);
                } else if ("/login".equals(request.getPath())) {
                    handleLoginRequest(request, response);
                } else {
                    response.send(404, "Not Found");
                }
                break;
            case "PUT":
                if ("/update".equals(request.getPath())) {
                    handlePutUpdate(request, response);
                } else {
                    response.send(404, "Not Found");
                }
                break;
            case "PATCH":
                if ("/modify".equals(request.getPath())) {
                    handlePatchModify(request, response);
                } else {
                    response.send(404, "Not Found");
                }
                break;
            case "DELETE":
                if ("/delete".equals(request.getPath())) {
                    handleDelete(request, response);
                } else {
                    response.send(404, "Not Found");
                }
                break;
            default:
                response.send(404, "Not Found");
        }
    }


    private void handlePostSubmit(HttpRequest request, HttpResponse response) throws IOException {
        if (!"application/json".equals(request.getHeaders().get("Content-Type"))) {
            response.send(415, "Unsupported Media Type");
            return;
        }

        String requestBody = request.getBody();
        try {
            Map<String, String> newData = gson.fromJson(requestBody, new TypeToken<Map<String, String>>(){}.getType());
            if (newData != null && newData.containsKey("key") && newData.containsKey("value")) {
                JsonObject jsonObject = JsonParser.parseString(newData.get("value")).getAsJsonObject();
                dataStore.put(newData.get("key"), jsonObject);
                if (flagForLongTimeout) {
                    simulateLongOperation();
                }
                response.send(201, "New entry added: " + newData.get("key") + " = " + jsonObject.toString());
            } else {
                response.send(400, "Invalid data format. Expected JSON with 'key' and 'value'.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.send(400, "Invalid JSON format.");
        }
    }

    private void handlePutUpdate(HttpRequest request, HttpResponse response) throws IOException {
        if (!"application/json".equals(request.getHeaders().get("Content-Type"))) {
            response.send(415, "Unsupported Media Type");
            return;
        }

        String requestBody = request.getBody();
        try {
            Map<String, String> updatedData = gson.fromJson(requestBody, new TypeToken<Map<String, String>>(){}.getType());
            if (updatedData != null && updatedData.containsKey("key") && updatedData.containsKey("value")) {
                if (dataStore.containsKey(updatedData.get("key"))) {
                    JsonObject jsonObject = JsonParser.parseString(updatedData.get("value")).getAsJsonObject();
                    dataStore.put(updatedData.get("key"), jsonObject);
                    if (flagForLongTimeout) {
                        simulateLongOperation();
                    }
                    response.send(200, "Updated entry: " + updatedData.get("key") + " = " + jsonObject.toString());
                } else {
                    response.send(404, "Data not found for key: " + updatedData.get("key"));
                }
            } else {
                response.send(400, "Invalid data format. Expected JSON with 'key' and 'value'.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.send(400, "Invalid JSON format.");
        }
    }

    private void handlePatchModify(HttpRequest request, HttpResponse response) throws IOException {
        if (!"application/json".equals(request.getHeaders().get("Content-Type"))) {
            response.send(415, "Unsupported Media Type");
            return;
        }

        String requestBody = request.getBody();
        try {
            Map<String, String> modifiedData = gson.fromJson(requestBody, new TypeToken<Map<String, String>>(){}.getType());
            if (modifiedData != null && modifiedData.containsKey("key") && modifiedData.containsKey("value")) {
                if (dataStore.containsKey(modifiedData.get("key"))) {
                    JsonObject existingObject = dataStore.get(modifiedData.get("key"));
                    JsonObject modifications = JsonParser.parseString(modifiedData.get("value")).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : modifications.entrySet()) {
                        existingObject.add(entry.getKey(), entry.getValue());
                    }
                    dataStore.put(modifiedData.get("key"), existingObject);
                    if (flagForLongTimeout) {
                        simulateLongOperation();
                    }
                    response.send(200, "Modified entry: " + modifiedData.get("key") + " = " + existingObject.toString());
                } else {
                    response.send(404, "Data not found for key: " + modifiedData.get("key"));
                }
            } else {
                response.send(400, "Invalid data format. Expected JSON with 'key' and 'value'.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.send(400, "Invalid JSON format.");
        }
    }

    private void handleDelete(HttpRequest request, HttpResponse response) throws IOException {
        String requestBody = request.getBody();
        try {
            Map<String, String> deleteData = gson.fromJson(requestBody, new TypeToken<Map<String, String>>(){}.getType());
            if (deleteData != null && deleteData.containsKey("key")) {
                if (dataStore.remove(deleteData.get("key")) != null) {
                    if (flagForLongTimeout) {
                        simulateLongOperation();
                    }
                    response.send(200, "Deleted entry with key: " + deleteData.get("key"));
                } else {
                    response.send(404, "Data not found for key: " + deleteData.get("key"));
                }
            } else {
                response.send(400, "Invalid data format. Expected JSON with 'key'.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.send(400, "Invalid JSON format.");
        }
    }

    private void handleExternalRequest(HttpRequest request, HttpResponse response) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url;
            if(flagForExternalServiceAvailable) {
                url = new URL("https://jsonplaceholder.typicode.com/posts/1");
            } else {
                url = new URL("https://jsonplaceholder.typicode.com/post/1");
            }

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                response.send(200, content.toString());
            } else {
                response.send(502, "Bad Gateway");
            }
        } catch (Exception e) {
            response.send(502, "Bad Gateway");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void handleSecureRequest(HttpRequest request, HttpResponse response) throws IOException {
        String token = request.getHeaders().get("Authorization");

        if (token == null || !AuthService.isAuthenticated(token)) {
            response.send(401, "Unauthorized");
            return;
        }

        if (!AuthService.isAuthorized(token)) {
            response.send(403, "Forbidden");
            return;
        }

        response.send(200, "You have access to secure data!");
    }

    private void handleSecureAdminRequest(HttpRequest request, HttpResponse response) throws IOException {
        String token = request.getHeaders().get("Authorization");

        if (token == null || !AuthService.isAuthenticated(token)) {
            response.send(401, "Unauthorized");
            return;
        }

        if (!AuthService.isAdmin(token)) {
            response.send(403, "Forbidden");
            return;
        }

        response.send(200, "You have access to admin data!");
    }

    private void handleSecureUserRequest(HttpRequest request, HttpResponse response) throws IOException {
        String token = request.getHeaders().get("Authorization");

        if (token == null || !AuthService.isAuthenticated(token)) {
            response.send(401, "Unauthorized");
            return;
        }

        response.send(200, "You have access to user data!");
    }

    private void handleRegisterRequest(HttpRequest request, HttpResponse response) throws IOException {
        String requestBody = request.getBody();
        Map<String, String> credentials = gson.fromJson(requestBody, new TypeToken<Map<String, String>>(){}.getType());

        String username = credentials.get("username");
        String password = credentials.get("password");

        if ("admin".equals(username) && "admin".equals(password)) {
            String token = UUID.randomUUID().toString();
            AuthService.registerToken(token, true);
            response.send(200, "Registration successful. Token: " + token);
        } else {
            response.send(200, "Registration successful for user: " + username);
        }
    }

    private void handleLoginRequest(HttpRequest request, HttpResponse response) throws IOException {
        String requestBody = request.getBody();
        Map<String, String> credentials = gson.fromJson(requestBody, new TypeToken<Map<String, String>>(){}.getType());

        String username = credentials.get("username");
        String password = credentials.get("password");

        if ("admin".equals(username) && "admin".equals(password)) {
            String token = UUID.randomUUID().toString();
            AuthService.registerToken(token, true);
            response.send(200, "Login successful. Token: " + token);
        } else if ("user1".equals(username) && "password1".equals(password)) {
            String token = UUID.randomUUID().toString();
            AuthService.registerToken(token, false);
            response.send(200, "Login successful. Token: " + token);
        } else {
            response.send(401, "Invalid credentials");
        }
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

            // Обработка тела запроса в отдельном потоке
            executorService.submit(() -> {
                try {
                    // Добавить задержку в 5 секунд после получения тела запроса
                    Thread.sleep(5000);

                    // Обработка тела запроса и отправка окончательного ответа
                    response.send(200, "Received data: " + requestBody);
                    System.out.println("Sent final response: Received data: " + requestBody);

                    // Закрытие канала после отправки окончательного ответа
                    request.getClientChannel().close();
                    System.out.println("Client channel closed after sending final response");
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                    try {
                        response.send(500, "Internal Server Error");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } else {
            response.send(417, "Expectation Failed");
            System.out.println("Expectation Failed");
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
        selector.close(); // Закрываем селектор после чтения данных
        return requestBody.toString();
    }

    private void handleRedirect(HttpRequest request, HttpResponse response) throws IOException {
        String location = "http://example.com";
        String responseBody = "Found: " + location;
        String responseHeaders = "HTTP/1.1 302 Found\r\n" +
                "Location: " + location + "\r\n" +
                "Content-Length: " + responseBody.length() + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                responseBody;
        ByteBuffer buffer = ByteBuffer.wrap(responseHeaders.getBytes());
        request.getClientChannel().write(buffer);
        request.getClientChannel().close();
    }



    private void simulateLongOperation() {
        try {
            Thread.sleep(11000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
