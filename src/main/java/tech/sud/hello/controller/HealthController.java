package tech.sud.hello.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.sud.hello.service.AuthService;

import java.util.Collections;
import java.util.Map;

/** Lightweight liveness endpoint that does not generate or expose tokens. */
@RestController
public class HealthController {
    @GetMapping("/health")
    public Map<String, Object> health() {
        return AuthService.success(Collections.singletonMap("status", "ok")).body();
    }
}
