package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MultipartParser {
    private final String boundary;
    private final InputStream inputStream;

    public MultipartParser(String contentType, InputStream inputStream) {
        this.boundary = contentType.split("boundary=")[1];
        this.inputStream = inputStream;
    }

    public List<Map<String, String>> parse() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        List<Map<String, String>> parts = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("--" + boundary)) {
                Map<String, String> part = new HashMap<>();
                while (!(line = reader.readLine()).isEmpty()) {
                    String[] header = line.split(": ");
                    if (header.length == 2) {
                        part.put(header[0], header[1]);
                    }
                }
                StringBuilder body = new StringBuilder();
                while (!(line = reader.readLine()).startsWith("--" + boundary)) {
                    body.append(line).append("\r\n");
                }
                part.put("body", body.toString().trim());
                parts.add(part);
            }
        }
        return parts;
    }
}
