package org.example.handlers;

import org.example.http.HttpRequest;
import org.example.http.HttpResponse;

import java.io.IOException;

/**
 * HttpHandler is an interface for handling HTTP requests and responses.
 * Implementations of this interface define how to process incoming HTTP requests
 * and generate appropriate HTTP responses.
 */

public interface HttpHandler {
    void handle(HttpRequest request, HttpResponse response) throws IOException;
}
