package org.example;

import java.io.IOException;

public interface HttpHandler {
    void handle(HttpRequest request, HttpResponse response) throws IOException;
}
