package tech.sud.hello;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import tech.sud.auth.gip.auth.SudGIPAuth;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/** Exercises real HTTP requests against the published SDK with public dummy credentials. */
@org.springframework.boot.test.context.SpringBootTest(
        webEnvironment =
                org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"sud.app-id=test-app", "sud.app-secret=0123456789abcdef0123456789abcdef"})
class DemoServerTest {
    static final String SECRET = "0123456789abcdef0123456789abcdef";
    static final ObjectMapper JSON = new ObjectMapper();

    @org.springframework.boot.test.web.server.LocalServerPort private int port;

    /** Minimal Java 8 HTTP client used only by integration tests. */
    static final class Response {
        private final int status;
        private final String body;

        Response(int status, String body) {
            this.status = status;
            this.body = body;
        }

        int statusCode() {
            return status;
        }

        String body() {
            return body;
        }
    }

    Response request(String method, String path, String body) throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        try {
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            // HttpURLConnection exposes error responses through a separate stream.
            InputStream stream =
                    status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (stream != null) {
                try (InputStream input = stream) {
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        bytes.write(buffer, 0, count);
                    }
                }
            }
            return new Response(status, new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            connection.disconnect();
        }
    }

    JsonNode success(Response r) throws Exception {
        assertEquals(200, r.statusCode());
        JsonNode n = JSON.readTree(r.body());
        assertEquals(0, n.path("ret_code").asInt(-1));
        return n.path("data");
    }

    @Test
    void completeFlow() throws Exception {
        String code =
                success(request("GET", "/api/getCode?uid=test-user-001", null))
                        .get("code")
                        .asText();
        JsonNode data =
                success(
                        request(
                                "POST",
                                "/api/getSSToken",
                                JSON.writeValueAsString(Collections.singletonMap("code", code))));
        assertEquals("test-user-001", data.path("user_info").path("uid").asText());
        // Advance past second-level token timestamps before checking refreshed expiry.
        Thread.sleep(1100);
        JsonNode refreshed =
                success(
                        request(
                                "POST",
                                "/api/updateSSToken",
                                JSON.writeValueAsString(
                                        Collections.singletonMap(
                                                "ss_token", data.path("ss_token").asText()))));
        assertTrue(refreshed.path("expire_date").asLong() > data.path("expire_date").asLong());
        assertTrue(
                new SudGIPAuth("test-app", SECRET)
                        .getUidBySSToken(refreshed.path("ss_token").asText())
                        .isSuccess());
    }

    @Test
    void invalidTokens() throws Exception {
        for (String[] pair :
                new String[][] {{"getSSToken", "code"}, {"updateSSToken", "ss_token"}}) {
            Response r =
                    request("POST", "/api/" + pair[0], "{\"" + pair[1] + "\":\"invalid-token\"}");
            assertEquals(400, r.statusCode());
            assertEquals(1004, JSON.readTree(r.body()).path("sdk_error_code").asInt());
        }
    }

    @Test
    void missingInputs() throws Exception {
        assertEquals(400, request("GET", "/api/getCode", null).statusCode());
        assertEquals(400, request("POST", "/api/getSSToken", "{}").statusCode());
        assertEquals(400, request("POST", "/api/updateSSToken", "{}").statusCode());
    }

    @Test
    void invalidJson() throws Exception {
        for (String body : new String[] {"{", "[]", "{\"code\":1}", "{} {}"}) {
            assertEquals(400, request("POST", "/api/getSSToken", body).statusCode());
        }
    }

    @Test
    void routing() throws Exception {
        assertEquals(200, request("GET", "/health", null).statusCode());
        assertEquals(404, request("GET", "/", null).statusCode());
        assertEquals(405, request("GET", "/api/getSSToken", null).statusCode());
    }

    @Test
    void unicodeUid() throws Exception {
        String code =
                success(request("GET", "/api/getCode?uid=%E7%94%A8%E6%88%B7%2F001", null))
                        .path("code")
                        .asText();
        assertEquals("用户/001", new SudGIPAuth("test-app", SECRET).getUidByCode(code).getUid());
    }

    @Test
    void expiredCode() throws Exception {
        String code = new SudGIPAuth("test-app", SECRET).getCode("u", 1).getCode();
        // SDK expiration checks use seconds, so cross the boundary deterministically.
        Thread.sleep(2100);
        Response result =
                request(
                        "POST",
                        "/api/getSSToken",
                        JSON.writeValueAsString(Collections.singletonMap("code", code)));
        assertEquals(400, result.statusCode());
        assertEquals(1005, JSON.readTree(result.body()).path("sdk_error_code").asInt());
    }

    @Test
    void wrongKey() throws Exception {
        String code =
                new SudGIPAuth("test-app", "different-key-for-testing-only").getCode("u").getCode();
        assertEquals(
                400,
                request(
                                "POST",
                                "/api/getSSToken",
                                JSON.writeValueAsString(Collections.singletonMap("code", code)))
                        .statusCode());
    }
}
