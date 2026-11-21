package tech.sud.hello.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Configures strict JSON inputs and prevents caching of authentication responses. */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer strictJson() {
        return builder ->
                builder.featuresToEnable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                        .postConfigurer(
                                mapper -> {
                                    // Reject numeric and boolean tokens instead of silently
                                    // converting them to strings.
                                    mapper.coercionConfigFor(LogicalType.Textual)
                                            .setCoercion(
                                                    CoercionInputShape.Integer, CoercionAction.Fail)
                                            .setCoercion(
                                                    CoercionInputShape.Float, CoercionAction.Fail)
                                            .setCoercion(
                                                    CoercionInputShape.Boolean,
                                                    CoercionAction.Fail);
                                });
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(
                new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(
                            HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) {
                        response.setHeader("Cache-Control", "no-store");
                        return true;
                    }
                });
    }
}
