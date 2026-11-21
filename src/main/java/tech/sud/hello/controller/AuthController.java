package tech.sud.hello.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tech.sud.hello.dto.CodeRequest;
import tech.sud.hello.dto.SSTokenRequest;
import tech.sud.hello.service.AuthService;

import java.util.Map;

/** Maps HTTP inputs to SDK use cases; token logic remains in AuthService. */
@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    /** In production, obtain the UID from the authenticated caller's session. */
    @GetMapping("/getCode")
    public ResponseEntity<Map<String, Object>> getCode(@RequestParam(required = false) String uid) {
        return response(service.getCode(uid));
    }

    /** Spring binds the JSON body; the service verifies the Code before reading its UID. */
    @PostMapping(value = "/getSSToken", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> getSSToken(@RequestBody CodeRequest request) {
        return response(service.getSSToken(request.getCode()));
    }

    /** Refresh uses the verified UID and does not revoke the previous token. */
    @PostMapping(value = "/updateSSToken", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> updateSSToken(@RequestBody SSTokenRequest request) {
        return response(service.updateSSToken(request.getSsToken()));
    }

    private ResponseEntity<Map<String, Object>> response(AuthService.Reply reply) {
        return ResponseEntity.status(reply.status()).body(reply.body());
    }
}
