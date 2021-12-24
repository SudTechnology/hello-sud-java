package tech.sud.hello.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.sud.hello.constants.CommonConstant;
import tech.sud.mgp.auth.api.SudMGPAuth;

/**
 * SudMGPAuth配置类
 *
 * @author Sud
 */
@Configuration
public class SudMGPAuthConfig {

    @Bean
    public SudMGPAuth sudMGPAuth() {
        return new SudMGPAuth(CommonConstant.APP_ID, CommonConstant.APP_SECRET);
    }
}
