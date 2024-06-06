# HTTP_Server

## Overview

This project is a simple HTTP server implemented in Java using non-blocking I/O (NIO). It handles various HTTP methods and provides authentication and authorization capabilities.

## Project Structure

- org.example.auth: Contains the AuthService class for managing authentication and authorization tokens.
- org.example.handlers: Contains handlers for processing different HTTP requests.
- org.example.http: Contains HttpRequest and HttpResponse classes representing HTTP requests and responses.
- org.example.server: Contains classes for starting and managing the HTTP server.

## Running the Server

To start the server, follow these steps:

1. Ensure you have Java installed on your machine.
2. Compile the project using your preferred method (e.g., Maven, Gradle, or directly using javac).
3. Run the ServerApp class to start the server.

```sh
java -cp target/classes org.example.ServerApp
```

Upon running the server, it will start on localhost at port 8081.

## Server Behavior

### On Start

- Initializes the HTTP server on localhost:8081.
- Registers various request handlers.
- Sets flags to simulate long-term operations and server availability.
- Starts the server and begins listening for incoming HTTP requests.
- Adds a shutdown hook to handle graceful server shutdown.

### On Shutdown

- The server stops listening for new connections.
- Active connections are closed.
- A message "Server closed. Goodbye!" is printed to the console.

The flags that are set are mainly needed in order to stimulate the server to work in different modes and handle different statuses. Short delays were used to check the operation of the server, while several users were connected at the same time.

## Supported HTTP Methods and Status Codes

The server supports the following HTTP methods and returns appropriate status codes based on the request and processing results.

### GET Methods

- /: Returns 200 OK with a welcome message.
- /data: Returns 200 OK with example data from the data store.
- /external: Fetches data from an external service and returns 200 OK or 502 Bad Gateway if the external service is unavailable.
- /secure/user: Requires an authenticated user token and returns 200 OK with user data or 401 Unauthorized.
- /secure/admin: Requires an authenticated admin token and returns 200 OK with admin data or 401 Unauthorized/403 Forbidden.

### POST Methods

- /submit: Accepts JSON data and adds it to the data store. Returns 201 Created or 400 Bad Request/415 Unsupported Media Type.
- /register: Registers a new user or admin and returns 200 OK with a token.
- /login: Authenticates a user or admin and returns 200 OK with a token or 401 Unauthorized

### PUT Methods

- /update: Updates existing data in the data store. Returns 200 OK or 404 Not Found/415 Unsupported Media Type.

### PATCH Methods

- /modify: Modifies existing data in the data store. Returns 200 OK or 404 Not Found/415 Unsupported Media Type.

### DELETE Methods

- /delete: Deletes data from the data store. Returns 200 OK or 404 Not Found.

### Other Methods

- /continue: Handles Expect: 100-continue requests and sends intermediate and final responses.
- /redirect: Sends a 302 Found response with a location header for redirection.

### Error Handling

- 501 Not Implemented: Returned for unsupported HTTP methods.
- 504 Gateway Timeout: Returned for requests that take too long to process.
- 505 HTTP Version Not Supported: Returned for unsupported HTTP versions.

## Example Curl Commands

```sh
curl -X POST -H "Content-Type: application/json" -d "{\"key\":\"testKey\",\"value\":\"{\\\"field1\\\":\\\"value1\\\", \\\"field2\\\":\\\"value2\\\"}\"}" http://localhost:8081/submit
```

```sh
curl -X PUT http://localhost:8081/update -H "Content-Type: application/json" -d "{\"key\":\"testKey\",\"value\":\"{\\\"field1\\\":\\\"newValue1\\\", \\\"field2\\\":\\\"newValue2\\\"}\"}"
```

```sh
curl -X PATCH http://localhost:8081/modify -H "Content-Type: application/json" -d "{\"key\":\"testKey\",\"value\":\"{\\\"field1\\\":\\\"modifiedValue1\\\"}\"}"
```

```sh
curl -X DELETE http://localhost:8081/delete -H "Content-Type: application/json" -d "{\"key\":\"testKey\"}"
```

```sh
curl -X GET http://localhost:8081/data
```

```sh
curl -X GET http://localhost:8081/ -0
```

```sh
curl -X POST -H "Content-Type: text/plain" -d "key=value" http://localhost:8081/submit
```

```sh
curl -v http://localhost:8081/redirect
```

```sh
curl -X GET http://localhost:8081/external
```

```sh
curl -X POST -H "Content-Type: application/json" -d "{\"username\": \"admin\", \"password\": \"admin\"}" http://localhost:8081/register
```

```sh
curl -X POST -H "Content-Type: application/json" -d "{\"username\": \"user1\", \"password\": \"password1\"}" http://localhost:8081/register
```

```sh
curl -v -H "Authorization: <admin-token>" http://localhost:8081/secure/admin
```

```sh
curl -v -H "Authorization: <user-token>" http://localhost:8081/secure/admin
```
