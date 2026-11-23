package tech.sud.hello.service;

import org.springframework.stereotype.Service;

import tech.sud.auth.gip.auth.SudGIPAuth;
import tech.sud.auth.gip.auth.model.CodeResponse;
import tech.sud.auth.gip.auth.model.SSTokenResponse;
import tech.sud.auth.gip.auth.model.UidResponse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demonstrates the SDK calls behind the three authentication endpoints. The SDK owns token
 * operations; this class owns HTTP result mapping and demo user data.
 */
@Service
public final class AuthService {
    private final SudGIPAuth auth;

    /** Reuse the configured SDK instance rather than constructing one per request. */
    public AuthService(SudGIPAuth auth) {
        this.auth = auth;
    }

    /** Carries the HTTP status separately from the JSON response body. */
    public static final class Reply {
        private final int status;
        private final Map<String, Object> body;

        public Reply(int status, Map<String, Object> body) {
            this.status = status;
            this.body = body;
        }

        public int status() {
            return status;
        }

        public Map<String, Object> body() {
            return body;
        }
    }

    /** Wrap an SDK result using the demo server's response contract. */
    public static Reply success(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ret_code", 0);
        body.put("ret_msg", "");
        body.put("sdk_error_code", 0);
        body.put("data", data);
        return new Reply(200, body);
    }

    /** Preserve SDK error codes; use zero for errors detected before calling the SDK. */
    public static Reply error(int status, String message, int sdkCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ret_code", 1);
        body.put("ret_msg", sdkCode == 0 ? message : "");
        body.put("sdk_error_code", sdkCode);
        body.put("data", Collections.emptyMap());
        // SDK business failures use HTTP 200; callers must inspect the response envelope.
        return new Reply(sdkCode == 0 ? status : 200, body);
    }

    private static boolean missing(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Generate a Code for a UID obtained by the application. */
    public Reply getCode(String uid) {
        if (missing(uid)) {
            return error(400, "Missing uid parameter", 0);
        }

        // Generation can fail too: inspect the response before reading its token.
        CodeResponse result = auth.getCode(uid);
        if (!result.isSuccess()) {
            return error(200, "", result.getErrorCode());
        }
        return success(Collections.singletonMap("code", result.getCode()));
    }

    /** Verify a Code, resolve its UID, and issue an SSToken for that user. */
    public Reply getSSToken(String code) {
        if (missing(code)) {
            return error(400, "Missing code parameter", 0);
        }

        // Never trust a UID decoded from an unverified token.
        UidResponse user = auth.getUidByCode(code);
        if (!user.isSuccess()) {
            return error(200, "", user.getErrorCode());
        }
        if (missing(user.getUid())) {
            return error(200, "", 1004);
        }

        SSTokenResponse token = auth.getSSToken(user.getUid());
        if (!token.isSuccess()) {
            return error(200, "", token.getErrorCode());
        }

        // Replace these fixed values with a lookup in your own user service.
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("uid", user.getUid());
        userInfo.put("nick_name", "Demo User");
        userInfo.put("avatar_url", "https://example.com/avatar.png");
        userInfo.put("gender", "female");
        userInfo.put("is_ai", 0);
        userInfo.put("ai_level", 0);

        Map<String, Object> data = tokenData(token);
        data.put("user_info", userInfo);
        return success(data);
    }

    /** Reissue an SSToken only after the existing token passes SDK verification. */
    public Reply updateSSToken(String ssToken) {
        if (missing(ssToken)) {
            return error(400, "Missing ss_token parameter", 0);
        }

        UidResponse user = auth.getUidBySSToken(ssToken);
        if (!user.isSuccess()) {
            return error(200, "", user.getErrorCode());
        }
        if (missing(user.getUid())) {
            return error(200, "", 1004);
        }

        // Issuing a new token does not revoke the previous one.
        SSTokenResponse token = auth.getSSToken(user.getUid());
        if (!token.isSuccess()) {
            return error(200, "", token.getErrorCode());
        }
        return success(tokenData(token));
    }

    /** Convert the SDK's Date into the Unix milliseconds used by the HTTP contract. */
    private static Map<String, Object> tokenData(SSTokenResponse token) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ss_token", token.getToken());
        data.put("expire_date", token.getExpireDate().getTime());
        return data;
    }
}
