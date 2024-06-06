import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.example.handlers.HttpRequestHandler;
import org.example.server.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerTest {
    private static HttpServer server;

    @BeforeClass
    public static void setUp() throws IOException {
        server = new HttpServer("localhost", 8081);
        HttpRequestHandler handler = new HttpRequestHandler();
        handler.setServiceAvailable(true);
        handler.setExternalServiceAvailable(true);
        handler.registerHandlers(server);

        // Setting a flag to simulate long-term operations
        HttpRequestHandler.setFlagForLongTimeout(false);

        // Start the server
        new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void atestGetRoot() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet("http://localhost:8081/");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");  // Убедитесь, что разделители строк одинаковые
            assertEquals("200: OK\nHello, World!", responseBody.trim());
        }
    }

    @Test
    public void btestPostSubmit() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost("http://localhost:8081/submit");
        request.addHeader("Content-Type", "application/json");

        String jsonBody = "{\"key\":\"testKey\",\"value\":\"{\\\"field1\\\":\\\"value1\\\", \\\"field2\\\":\\\"value2\\\"}\"}";
        request.setEntity(new StringEntity(jsonBody));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");

            assertEquals("201: Created\nNew entry added: testKey = {\"field1\":\"value1\",\"field2\":\"value2\"}", responseBody.trim());
        }
    }

    @Test
    public void ctestPutUpdate() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPut request = new HttpPut("http://localhost:8081/update");
        request.addHeader("Content-Type", "application/json");

        String jsonBody = "{\"key\":\"testKey\",\"value\":\"{\\\"field1\\\":\\\"newValue1\\\", \\\"field2\\\":\\\"newValue2\\\"}\"}";
        request.setEntity(new StringEntity(jsonBody));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");

            assertEquals("200: OK\nUpdated entry: testKey = {\"field1\":\"newValue1\",\"field2\":\"newValue2\"}", responseBody.trim());
        }
    }

    @Test
    public void dtestPatchModify() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPatch request = new HttpPatch("http://localhost:8081/modify");
        request.addHeader("Content-Type", "application/json");

        String jsonBody = "{\"key\":\"testKey\",\"value\":\"{\\\"field1\\\":\\\"modifiedValue1\\\"}\"}";
        request.setEntity(new StringEntity(jsonBody));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");

            assertEquals("200: OK\nModified entry: testKey = {\"field1\":\"modifiedValue1\",\"field2\":\"newValue2\"}", responseBody.trim());
        }
    }

    @Test
    public void etestDelete() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete request = new HttpDelete("http://localhost:8081/delete");
        request.addHeader("Content-Type", "application/json");

        String jsonBody = "{\"key\":\"testKey\"}";
        request.setEntity(new StringEntity(jsonBody));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");

            assertEquals("200: OK\nDeleted entry with key: testKey", responseBody.trim());
        }
    }

    @Test
    public void ftestGetData() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet("http://localhost:8081/data");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");

            assertEquals("200: OK\n{\"example\":{\"field1\":\"value1\",\"field2\":\"value2\"}}", responseBody.trim());
        }
    }

    @Test
    public void gtestDeleteExample() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete request = new HttpDelete("http://localhost:8081/delete");
        request.addHeader("Content-Type", "application/json");

        String jsonBody = "{\"key\":\"example\"}";
        request.setEntity(new StringEntity(jsonBody));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");

            assertEquals("200: OK\nDeleted entry with key: example", responseBody.trim());
        }
    }

    @Test
    public void htestGetEmptyData() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet("http://localhost:8081/data");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");

            assertEquals("200: OK\n{}", responseBody.trim());
        }
    }

    @Test
    public void itestHttp09VersionNotSupported() throws IOException {
        try (Socket socket = new Socket("localhost", 8081)) {
            OutputStream os = socket.getOutputStream();
            os.write("GET / HTTP/0.9\r\n\r\n".getBytes());
            os.flush();

            byte[] buffer = new byte[1024];
            int read = socket.getInputStream().read(buffer);
            String response = new String(buffer, 0, read);

            // Extract only the body part of the response
            String[] responseParts = response.split("\r\n\r\n", 2);
            String responseBody = responseParts.length > 1 ? responseParts[1].trim() : "";

            // Normalize line separators to LF for comparison
            responseBody = responseBody.replace("\r\n", "\n").replace("\r", "\n");

            assertEquals("505: HTTP Version Not Supported\nHTTP Version not supported", responseBody);
        }
    }

    @Test
    public void jtestGatewayTimeout() throws IOException, ParseException {
        HttpRequestHandler.setFlagForLongTimeout(true); // Activate the long timeout flag

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost("http://localhost:8081/submit");
        request.addHeader("Content-Type", "application/json");

        String jsonBody = "{\"key\":\"testKey\",\"value\":\"{\\\"field1\\\":\\\"value1\\\", \\\"field2\\\":\\\"value2\\\"}\"}";
        request.setEntity(new StringEntity(jsonBody));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");  // Normalize line separators

            assertEquals("504: Gateway Timeout\nGateway Timeout", responseBody);
            HttpRequestHandler.setFlagForLongTimeout(false);
        }
    }

    @Test
    public void ktestGetExternal() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet("http://localhost:8081/external");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");  // Normalize line separators

            String expectedResponse = "200: OK\n" +
                    "{  \"userId\": 1,  \"id\": 1,  \"title\": \"sunt aut facere repellat provident occaecati excepturi optio reprehenderit\",  \"body\": \"quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto\"}";

            assertEquals(expectedResponse, responseBody);
        }
    }

    @Test
    public void ltestExternalServiceBadGateway() throws IOException {
        // Set the external service to unavailable
        HttpRequestHandler.setExternalServiceAvailable(false);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet("http://localhost:8081/external");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");  // Ensure line separators are consistent
            assertEquals("502: Bad Gateway\nBad Gateway", responseBody);
            HttpRequestHandler.setExternalServiceAvailable(true);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } finally {
            httpClient.close();
        }
    }

    @Test
    public void mtestUnsupportedHttpMethod() throws IOException, ParseException, URISyntaxException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        URI uri = new URI("http://localhost:8081/");
        HttpUriRequestBase request = new HttpUriRequestBase("GO", uri);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");  // Normalize line separators

            String expectedResponse = "501: Not Implemented\nNot Implemented";

            assertEquals(expectedResponse, responseBody);
        }
    }

    @Test
    public void ntestUnsupportedMediaType() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost("http://localhost:8081/submit");
        request.addHeader("Content-Type", "text/plain");

        String body = "key=value";
        request.setEntity(new StringEntity(body));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");  // Normalize line separators

            String expectedResponse = "415: Unsupported Media Type\nUnsupported Media Type";

            assertEquals(expectedResponse, responseBody);
        }
    }

    @Test
    public void otestDataNotFoundForPatch() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPatch request = new HttpPatch("http://localhost:8081/modify");
        request.addHeader("Content-Type", "application/json");

        String jsonBody = "{\"key\":\"example\",\"value\":\"{\\\"field1\\\":\\\"modifiedValue1\\\"}\"}";
        request.setEntity(new StringEntity(jsonBody));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity()).trim();
            responseBody = responseBody.replace("\r\n", "\n");  // Normalize line separators

            String expectedResponse = "404: Not Found\nData not found for key: example";

            assertEquals(expectedResponse, responseBody);
        }
    }
}
