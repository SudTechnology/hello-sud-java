package tech.sud.hello;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Fixed independent HMAC fixture protects byte-level compatibility with the documented recipe. */
@SpringBootTest(
        properties = {
            "sud.app-id=test-app",
            "sud.app-secret=0123456789abcdef0123456789abcdef",
            "sud.verify-callback-signature=true"
        })
@AutoConfigureMockMvc
class CallbackSignatureTest {
    private static final String BODY = "{ \"code\": \"invalid-token\", \"future_field\": true }";
    private static final String SIGNATURE = "8eff7505823b36f0b68fc0c1b8e6e6d859755a85";
    @Autowired private MockMvc mvc;

    private MockHttpServletRequestBuilder signed(String body) {
        return post("/api/getSSToken")
                .contentType("application/json")
                .content(body)
                .header("Sud-AppId", "test-app")
                .header("Sud-Timestamp", "1234567890")
                .header("Sud-Nonce", "nonce-001")
                .header("Sud-Signature", SIGNATURE);
    }

    @Test
    void validSignatureReachesSdkAndAllowsFutureFields() throws Exception {
        // The callback signature is valid, but the dummy token must still fail SDK verification.
        mvc.perform(signed(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sdk_error_code").value(1004));
    }

    @Test
    void changedWhitespaceInvalidatesSignature() throws Exception {
        mvc.perform(signed(BODY + " "))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.sdk_error_code").value(0));
    }

    @Test
    void missingHeadersFailClosed() throws Exception {
        mvc.perform(post("/api/getSSToken").contentType("application/json").content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/updateSSToken")
                                .contentType("application/json")
                                .content("{\"ss_token\":\"invalid-token\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void alteredSignatureIsRejected() throws Exception {
        mvc.perform(
                        post("/api/getSSToken")
                                .contentType("application/json")
                                .content(BODY)
                                .header("Sud-AppId", "test-app")
                                .header("Sud-Timestamp", "1234567890")
                                .header("Sud-Nonce", "nonce-001")
                                .header(
                                        "Sud-Signature",
                                        "0000000000000000000000000000000000000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    void callbackApisRequireSignatureBeforeRoutingOrParsing() throws Exception {
        mvc.perform(post("/api/getSSToken").contentType("application/json").content("{"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/futureEndpoint")).andExpect(status().isForbidden());
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                "/health"))
                .andExpect(status().isOk());
    }

    @Test
    void getCodeDoesNotRequireCallbackSignature() throws Exception {
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                "/api/getCode?uid=u"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").isString());
        // Callback headers are irrelevant to this endpoint, even if supplied incorrectly.
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/getCode?uid=u")
                                .header("Sud-Signature", "invalid"))
                .andExpect(status().isOk());
    }

    @Test
    void originalUtf8AndCrLfBytesAreVerifiedAndReplayed() throws Exception {
        String body = "{\r\n \"future_field\": \"用户\", \"code\": \"invalid-token\"\r\n}";
        mvc.perform(
                        post("/api/getSSToken")
                                .contentType("application/json")
                                .content(body.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                                .header("Sud-AppId", "test-app")
                                .header("Sud-Timestamp", "1234567890")
                                .header("Sud-Nonce", "nonce-001")
                                .header(
                                        "Sud-Signature",
                                        "b4d9275485655db2a69bbcbf0b816e9050908d54"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sdk_error_code").value(1004));
        // Same JSON values after newline normalization MUST fail with the original signature.
        mvc.perform(
                        post("/api/getSSToken")
                                .contentType("application/json")
                                .content(
                                        body.replace("\r\n", "\n")
                                                .getBytes(java.nio.charset.StandardCharsets.UTF_8))
                                .header("Sud-AppId", "test-app")
                                .header("Sud-Timestamp", "1234567890")
                                .header("Sud-Nonce", "nonce-001")
                                .header(
                                        "Sud-Signature",
                                        "b4d9275485655db2a69bbcbf0b816e9050908d54"))
                .andExpect(status().isForbidden());
    }
}
