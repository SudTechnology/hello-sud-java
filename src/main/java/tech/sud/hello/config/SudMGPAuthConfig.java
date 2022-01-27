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

    /**
     * 创建SUD服务端SDK鉴权对象
     *
     * @return SUD服务端SDK鉴权对象
     */
    @Bean
    public SudMGPAuth sudMGPAuth() {
        return new SudMGPAuth(CommonConstant.APP_ID, CommonConstant.APP_SECRET);
    }
}
