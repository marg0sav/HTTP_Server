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
        System.out.println("Starting to parse multipart data with boundary: " + boundary);

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("--" + boundary)) {
                System.out.println("Found boundary: " + line);
                Map<String, String> part = new HashMap<>();
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    System.out.println("Header line: " + line);
                    String[] header = line.split(": ");
                    if (header.length == 2) {
                        part.put(header[0], header[1]);
                        System.out.println("Parsed header: " + header[0] + " = " + header[1]);
                    }
                }
                StringBuilder body = new StringBuilder();
                while ((line = reader.readLine()) != null && !line.startsWith("--" + boundary)) {
                    body.append(line).append("\r\n");
                }
                part.put("body", body.toString().trim());
                System.out.println("Parsed body: " + body.toString().trim());
                parts.add(part);

                if (line != null && line.startsWith("--" + boundary + "--")) {
                    System.out.println("End of multipart data found");
                    break;
                }
            }
        }
        return parts;
    }
}
