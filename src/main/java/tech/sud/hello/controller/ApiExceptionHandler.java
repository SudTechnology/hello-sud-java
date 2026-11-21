package tech.sud.hello.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import tech.sud.hello.service.AuthService;

import java.util.Map;

/** Keeps framework errors in the same response envelope as SDK failures. */
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        HttpMediaTypeNotSupportedException.class
    })
    public ResponseEntity<Map<String, Object>> invalidRequest(Exception exception) {
        return error(400, "Invalid request");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound() {
        return error(404, "Not Found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> methodNotAllowed(
            HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(405)
                .allow(
                        exception
                                .getSupportedHttpMethods()
                                .toArray(new org.springframework.http.HttpMethod[0]))
                .body(AuthService.error(405, "Method Not Allowed", 0).body());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpectedFailure(Exception exception) {
        // Exception messages may contain credentials or tokens; never return them to callers.
        return error(500, "Internal Server Error");
    }

    private ResponseEntity<Map<String, Object>> error(int status, String message) {
        return ResponseEntity.status(status).body(AuthService.error(status, message, 0).body());
    }
}
