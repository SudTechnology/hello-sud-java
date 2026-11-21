package tech.sud.hello.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import tech.sud.hello.service.AuthService;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * Verifies /api requests before routing, DTO binding, or SDK calls when enabled. getCode issues
 * client Codes and is excluded; the separate /health endpoint is also excluded.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CallbackSignatureFilter extends OncePerRequestFilter {
    private final boolean enabled;
    private final String appId;
    private final byte[] secret;
    private final ObjectMapper json;

    public CallbackSignatureFilter(
            @Value("${sud.verify-callback-signature:false}") boolean enabled,
            @Value("${sud.app-id}") String appId,
            @Value("${sud.app-secret}") String secret,
            ObjectMapper json) {
        this.enabled = enabled;
        this.appId = appId;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        // getCode is a client-facing endpoint, not a signed server callback.
        return !enabled
                || path.equals("/api/getCode")
                || !(path.equals("/api") || path.startsWith("/api/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // SECURITY: Read the original HTTP entity-body bytes from the servlet input stream.
        // Do NOT parse JSON, deserialize a DTO, re-serialize, trim whitespace, normalize newlines,
        // reorder fields, or decode/re-encode the body before verification. Even semantically
        // equivalent JSON can have different bytes. The purpose is to detect any body tampering.
        // Servlet transport framing (for example chunk boundaries) is not part of the signed body.
        byte[] originalBody = StreamUtils.copyToByteArray(request.getInputStream());
        response.setHeader("Cache-Control", "no-store");
        if (!verify(request, originalBody)) {
            response.setStatus(403);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            json.writeValue(
                    response.getOutputStream(),
                    AuthService.error(403, "Invalid callback signature", 0).body());
            return;
        }
        // Replay exactly the verified bytes; downstream JSON conversion happens only after success.
        chain.doFilter(new OriginalBodyRequest(request, originalBody), response);
    }

    private boolean verify(HttpServletRequest request, byte[] body) {
        String id = request.getHeader("Sud-AppId");
        String timestamp = request.getHeader("Sud-Timestamp");
        String nonce = request.getHeader("Sud-Nonce");
        String signature = request.getHeader("Sud-Signature");
        if (!appId.equals(id)
                || missing(timestamp)
                || missing(nonce)
                || signature == null
                || !signature.matches("[0-9a-f]{40}")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            // All four lines, including the original body, end with a newline.
            mac.update(
                    (id + "\n" + timestamp + "\n" + nonce + "\n").getBytes(StandardCharsets.UTF_8));
            mac.update(body);
            byte[] digest = mac.doFinal(new byte[] {'\n'});
            byte[] supplied = new byte[20];
            for (int i = 0; i < supplied.length; i++) {
                supplied[i] = (byte) Integer.parseInt(signature.substring(i * 2, i * 2 + 2), 16);
            }
            if (!MessageDigest.isEqual(digest, supplied)) {
                return false;
            }
            return true;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Callback signature calculation failed", exception);
        }
    }

    private boolean missing(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Makes the consumed body available to Spring without modifying its bytes. */
    private static final class OriginalBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private OriginalBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public int read(byte[] buffer, int offset, int length) {
                    return input.read(buffer, offset, length);
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException(
                            "This demo uses synchronous request handling");
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            return new BufferedReader(
                    new InputStreamReader(getInputStream(), encoding == null ? "UTF-8" : encoding));
        }
    }
}
