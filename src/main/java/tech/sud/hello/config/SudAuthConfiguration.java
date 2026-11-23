package tech.sud.hello.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.sud.auth.gip.auth.RequestSigner;
import tech.sud.auth.gip.auth.SudGIPAuth;

/** Reads runtime credentials and creates one SDK instance for the application. */
@Configuration
public class SudAuthConfiguration {
    /** Reuse the SDK signer; HTTP body capture remains the filter's responsibility. */
    @Bean
    public RequestSigner requestSigner(
            @Value("${sud.app-id}") String appId, @Value("${sud.app-secret}") String appSecret) {
        return new RequestSigner(appId, appSecret);
    }

    @Bean
    public SudGIPAuth sudGIPAuth(
            @Value("${sud.app-id}") String appId, @Value("${sud.app-secret}") String appSecret) {
        // Fail during startup rather than accepting requests with invalid configuration.
        if (appId.trim().isEmpty() || appSecret.trim().isEmpty()) {
            throw new IllegalArgumentException("APP_ID and APP_SECRET must be configured");
        }
        return new SudGIPAuth(appId, appSecret);
    }
}
