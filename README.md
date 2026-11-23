# SudGIP Auth Server Demo

A Java 8 demo server showing how to use **tech.sud.gip:sud-gip-auth-java:1.1.0** to generate Codes, exchange them for SSTokens and user information, and refresh SSTokens. The server uses Spring Boot 2.7.18 and Spring MVC; authentication operations are delegated to the SDK.

The project targets JDK 8, so it uses Spring Boot 2.7.18. Spring Boot 2.x has reached the end of open-source support. This is a compatibility choice for this demo; plan a supported Java and Spring Boot upgrade before production deployment.

## Quick Start

Requires JDK 8 and Maven 3.9.x. Import `pom.xml` in IntelliJ IDEA and select JDK 8 for both the project SDK and Maven runner. Set `JAVA_HOME` to your JDK 8 installation before building from a terminal.

```bash
git clone https://github.com/SudTechnology/hello-sud-java.git
cd hello-sud-java
mvn verify
export APP_ID=local-test-app
export APP_SECRET="$(openssl rand -hex 32)"
java -jar target/hello-sud-java-1.1.0.jar
```

These random credentials are for isolated local testing only. Use issued application credentials for remote integration. In IDEA, run `tech.sud.hello.HelloSudApplication` with `APP_ID` and `APP_SECRET` set in the run configuration.

Open `http://127.0.0.1:8000/api/getCode?uid=test-user-001`. The health endpoint is `/health`. No homepage is configured at `/`. Press Ctrl+C to stop.

## Implementing the Demo Server

The SDK handles Token generation, verification, and UID extraction. Your server handles routing, request validation, HTTP responses, user profiles, and authorization.

### 1. Add the SDK and initialize it

```xml
<dependency>
  <groupId>tech.sud.gip</groupId>
  <artifactId>sud-gip-auth-java</artifactId>
  <version>1.1.0</version>
</dependency>
```

```java
import tech.sud.auth.gip.auth.SudGIPAuth;

SudGIPAuth auth = new SudGIPAuth(
    System.getenv("APP_ID"), System.getenv("APP_SECRET"));
```

`SudAuthConfiguration` checks configuration and registers the SDK as a Spring bean. Spring injects it into `AuthService`, which is injected into `AuthController`. Both configuration values must be nonblank. This SDK trims surrounding whitespace from them; use the exact issued credentials without extra whitespace. The demo does not impose a new minimum key length on top of the Java SDK.

### 2. Generate a Code

Inside `AuthService`:

```java
CodeResponse result = auth.getCode(uid);
if (!result.isSuccess()) {
    return AuthService.error(200, "", result.getErrorCode());
}
return AuthService.success(Collections.singletonMap("code", result.getCode()));
```

The demo accepts UID as a query parameter. In a real application, obtain it from an authenticated user session or validated access token. The SDK does not authenticate the HTTP caller.

### 3. Exchange a Code for an SSToken

After reading the JSON `code` field, verify it before using its UID:

```java
UidResponse user = auth.getUidByCode(code);
if (!user.isSuccess()) {
    return AuthService.error(200, "", user.getErrorCode());
}
SSTokenResponse token = auth.getSSToken(user.getUid());
if (!token.isSuccess()) {
    return AuthService.error(200, "", token.getErrorCode());
}
Map<String, Object> data = new LinkedHashMap<>();
data.put("ss_token", token.getToken());
data.put("expire_date", token.getExpireDate().getTime());
// Mock profile: keep the verified UID and replace the other fields with your user data.
Map<String, Object> userInfo = new LinkedHashMap<>();
userInfo.put("uid", user.getUid());
userInfo.put("nick_name", "Demo User");
userInfo.put("avatar_url", "https://example.com/avatar.png");
userInfo.put("gender", "female");
userInfo.put("is_ai", 0);
userInfo.put("ai_level", 0);
data.put("user_info", userInfo);
return AuthService.success(data);
```

Always check `isSuccess()` on generation and verification results. SDK expiration results are `java.util.Date`; `.getTime()` produces the Unix millisecond value used by this HTTP API.

The demo adds fixed nickname, avatar, and other profile fields. Replace those fields with your own user-service lookup. Only the UID is supplied by the verified Token.

### 4. Refresh an SSToken

```java
UidResponse user = auth.getUidBySSToken(ssToken);
if (!user.isSuccess()) {
    return AuthService.error(200, "", user.getErrorCode());
}
SSTokenResponse token = auth.getSSToken(user.getUid());
if (!token.isSuccess()) {
    return AuthService.error(200, "", token.getErrorCode());
}
Map<String, Object> data = new LinkedHashMap<>();
data.put("ss_token", token.getToken());
data.put("expire_date", token.getExpireDate().getTime());
return AuthService.success(data);
```

Refresh verifies the existing Token and signs a new one for the same UID. It does not revoke the old Token. The complete service also rejects missing or blank UIDs before generation.

### 5. Connect the SDK flow to HTTP

`AuthController` exposes the three endpoints using Spring MVC annotations. Request DTOs describe the JSON inputs, and constructor injection connects the controller to the service:

```java
@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping(value = "/getSSToken", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> getSSToken(@RequestBody CodeRequest request) {
        AuthService.Reply reply = service.getSSToken(request.getCode());
        return ResponseEntity.status(reply.status()).body(reply.body());
    }
}
```

The request flow is **Controller → Service → SDK**. `AuthService.Reply` carries the demo's HTTP status and JSON envelope; it is not an SDK type. `ApiExceptionHandler` uses `@RestControllerAdvice` to return the same envelope for malformed requests and framework errors. `WebConfiguration` rejects non-string token fields and trailing JSON, and adds `Cache-Control: no-store`.

The service snippets above use `java.util.Map`, `LinkedHashMap`, and `Collections`, plus response types from `tech.sud.auth.gip.auth.model`. See the source for complete imports and validation.

## Optional Callback Signature Verification

> **IMPORTANT — Verify the original request body before any deserialization or conversion.**
> Calculate the signature from the exact body bytes received through `HttpServletRequest.getInputStream()`.
> Do not parse JSON, bind a DTO, re-serialize an object, trim whitespace, reorder fields, normalize
> line endings, or decode and re-encode the body before verification. Even JSON with identical
> values can produce a different signature when its bytes change. This check is intended to
> detect tampering with the received body.

### Enable verification and understand its scope

`user_info` is mocked locally in `AuthService`. The verified UID is preserved; nickname, avatar,
gender, and AI fields are sample values. Replace this mapping with your user-service lookup as needed.

HTTP callback signature verification is optional and **disabled by default**. Enable it with
`VERIFY_CALLBACK_SIGNATURE=true` when configuring signed callbacks. It applies to `/api/**`
requests, including `getSSToken` and `updateSSToken`, before controller routing or request binding.
`/api/getCode` is explicitly excluded because it issues Codes to clients; `/health` is also excluded.
Missing or incorrect signatures prevent protected callback API execution. `getCode` works without
signature headers regardless of this setting; production applications should authenticate its caller.

### Preserve the original bytes throughout verification

The required processing order is:

1. Read and retain the original request body bytes in `CallbackSignatureFilter`.
2. Calculate and compare the callback signature using those bytes, before invoking Spring MVC.
3. Reject the request immediately if verification fails.
4. Only after verification succeeds, replay the **same, unchanged bytes** to Spring for JSON
   deserialization and normal Controller → Service → SDK processing.

Do not calculate a signature from `ObjectMapper.writeValueAsString(requestDto)` or any reconstructed
JSON. For example, `{"code":"abc"}` and `{ "code": "abc" }` contain the same JSON value but have
**different signed bytes**. The request wrapper exists to let downstream code read the verified body
again; it must never transform that body. Any upstream middleware must also preserve these bytes.

### Signature construction

`CallbackSignatureFilter` runs before Spring MVC. It checks `Sud-AppId` against the configured
application ID and requires `Sud-Timestamp`, `Sud-Nonce`, and `Sud-Signature`. The signature is the
lowercase hexadecimal HMAC-SHA1 of the following bytes, using `APP_SECRET` as the UTF-8 key:

```text
app_id\n
timestamp\n
nonce\n
original_request_body\n
```

For GET requests with no body, the body is zero bytes; the final newline still participates in the
signature. The documented signature recipe does not cover the URL, HTTP method, or query parameters; do not describe it as protection for those values.

**The final newline is required**, including when the body is empty. Append this protocol newline
when calculating the signature; do not trim or replace any newline already present in the body.
Whitespace, field order, character encoding, and line endings in the original body must remain unchanged. The request is then passed to normal JSON binding and SDK Code/SSToken verification.
Invalid or missing signatures return the demo's HTTP 403 error envelope with `sdk_error_code: 0`.
This verifies request integrity; it does not implement a timestamp freshness window or nonce replay
store. These policies are not specified by the supplied signature recipe.

### SDK-backed callback verification

The demo uses `RequestSigner` from SDK **1.1.0**. `SudAuthConfiguration` creates the signer;
`CallbackSignatureFilter` only captures the original bytes and headers, rejects duplicate headers,
and calls `verifySignature`. The HMAC algorithm belongs to the SDK.

**Verify the exact original body BEFORE JSON deserialization or any conversion.** Do not sign a
reconstructed DTO or JSON string. After verification, the filter replays the same bytes for Spring MVC.
`VERIFY_CALLBACK_SIGNATURE=true` protects `/api/**` except `/api/getCode`; `/health` is also exempt.
Missing or invalid callback signatures return HTTP 403, `ret_code: 1`, and `sdk_error_code: 0`.
Outgoing calls to Sud APIs can use the SDK's `generateAuthorization(timestamp, nonce, rawBody)`;
this is distinct from the incoming callback's four `Sud-*` headers.

An expired Code or SSToken returns **HTTP 200** with this business error envelope:

```json
{"ret_code":1,"ret_msg":"","sdk_error_code":1005,"data":{}}
```

Other SDK failures also return HTTP 200 with their SDK error code. Request validation, routing,
and unexpected server failures retain their respective HTTP error statuses.

## API Reference

POST requests require `Content-Type: application/json`. Successful responses and SDK business failures use HTTP 200. Always inspect `ret_code` and `sdk_error_code`; HTTP 200 alone does not mean the operation succeeded. Invalid request syntax or missing fields use HTTP 400.

### getCode

`GET /api/getCode?uid={uid}`

```json
{"ret_code":0,"ret_msg":"","sdk_error_code":0,"data":{"code":"generated_code"}}
```

### getSSToken

`POST /api/getSSToken`

```json
{"code":"code_from_getCode"}
```

Response:

```json
{
  "ret_code":0,
  "ret_msg":"",
  "sdk_error_code":0,
  "data":{
    "ss_token":"generated_sstoken",
    "expire_date":1795257193000,
    "user_info":{
      "uid":"test-user-001",
      "nick_name":"Demo User",
      "avatar_url":"https://example.com/avatar.png",
      "gender":"female",
      "is_ai":0,
      "ai_level":0
    }
  }
}
```

### User profile fields

The `getSSToken` response includes the complete `user_info` object shown above.
`uid` comes from the verified Code; all other values are mocked by this demo server,
not generated by the SDK. Replace mock values with your own user-service data.

| Field | Type | Required | Demo value / meaning |
|---|---|---|---|
| `uid` | string | Yes | Verified user ID; maximum 200 bytes |
| `nick_name` | string | Yes | `"Demo User"` |
| `avatar_url` | string | Yes | `"https://example.com/avatar.png"` (placeholder; replace with a real avatar URL) |
| `gender` | string | Yes | `"female"`; use `"male"` or `""` for unknown gender as appropriate |
| `is_ai` | int32 | No | `0` for a regular user, `1` for an AI user; default `0` |
| `ai_level` | int32 | No | `0` in this demo; `0`/`1` simple, `2` intermediate, `3` advanced; default `0` |

`updateSSToken` returns only the renewed token and expiry, without `user_info`.

### updateSSToken

`POST /api/updateSSToken`

```json
{"ss_token":"current_sstoken"}
```

Response:

```json
{"ret_code":0,"ret_msg":"","sdk_error_code":0,"data":{"ss_token":"new_sstoken","expire_date":1795257312000}}
```

### Errors

```json
{"ret_code":1,"ret_msg":"Token verification failed","sdk_error_code":1004,"data":{}}
```

`ret_code` and `ret_msg` belong to the demo. `sdk_error_code` preserves the SDK's error code, or is 0 for request-level errors. A malformed Token such as `invalid-token` returns 1004 with this SDK. Expired Tokens return 1005. Do not infer exact codes from other JWT implementations.

Missing fields, invalid JSON, wrong field types, and non-JSON POST bodies return 400. Unknown routes return 404, wrong HTTP methods return 405, and unexpected failures return a generic 500 response. Secrets and Token values are not logged.

## Configuration and Docker

| Variable | Description | Default |
|---|---|---|
| APP_ID | Issued application ID | Required |
| APP_SECRET | Issued application secret | Required |
| HOST | Bind address | 127.0.0.1; 0.0.0.0 in Docker |
| PORT | HTTP port | 8000 |
| VERIFY_CALLBACK_SIGNATURE | Verify signed callback requests | false |

The application does not automatically read `.env`. Docker can supply it as environment variables:

```bash
cp .env.example .env
# Edit .env with your application credentials.
docker build -t hello-sud-java:test .
docker run --rm --name hello-sud-java-test \
  -p 127.0.0.1:8000:8000 --env-file .env hello-sud-java:test
```

`.env` is excluded from Git and the Docker image. Recreate the container after changing configuration. Rebuild after changing code or dependencies. Never commit issued secrets.

## Testing

```bash
mvn verify
```

Spring Boot integration tests start embedded Tomcat on an automatically selected loopback port using dummy credentials. They automatically pass Code and SSToken between requests, verify UID and extended expiry, and cover malformed Tokens, missing inputs, invalid JSON, routing, Unicode UIDs, and wrong keys. No running demo instance or manual Token copying is needed. Test failures fail the build. GitHub Actions runs the same command.

## Project Structure

```text
hello-sud-java/
├── src/main/java/tech/sud/hello/
│   ├── HelloSudApplication.java       # Spring Boot entry point
│   ├── config/
│   │   ├── SudAuthConfiguration.java  # SDK bean and credentials
│   │   ├── CallbackSignatureFilter.java # Optional raw-body signature verification
│   │   └── WebConfiguration.java      # JSON and cache behavior
│   ├── controller/
│   │   ├── AuthController.java        # Three authentication endpoints
│   │   ├── HealthController.java      # Liveness endpoint
│   │   └── ApiExceptionHandler.java   # Common error responses
│   ├── dto/                          # Code and SSToken request bodies
│   └── service/AuthService.java      # SDK calls and response mapping
├── src/main/resources/application.properties
├── src/test/java/tech/sud/hello/DemoServerTest.java
├── .github/workflows/tests.yml
├── .env.example
├── .dockerignore
├── Dockerfile
├── pom.xml
└── README.md
```

## Integration Notes

SDK 1.1.0 defaults to a one-hour Code and a two-hour SSToken. Custom duration arguments are **seconds**, for example `auth.getCode(uid, 1800)`. HTTP `expire_date` values are **milliseconds**.

Use the SDK for both generation and verification. Version 1.1.0 retains its own signature encoding; do not assume its Tokens can be verified by a generic JWT library. Code and SSToken share a format, and the SDK does not compare the embedded application ID with the configured one. Your service must enforce the required user and application authorization rules.

This demo has no login system, persistence, revocation store, or production rate limiting. Local tests validate SDK integration within this server; they do not replace remote-service testing with issued credentials.

## References

- [Spring Boot 2.7.18 Java requirements](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/getting-started.html#getting-started.system-requirements)
- [Spring Boot 2.x support status](https://spring.io/blog/2023/11/23/spring-boot-2-7-18-available-now/)
- SDK: https://github.com/SudTechnology/sud-gip-auth-java
- Maven coordinates: `tech.sud.gip:sud-gip-auth-java:1.1.0`
