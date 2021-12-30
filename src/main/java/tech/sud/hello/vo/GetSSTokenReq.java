package tech.sud.hello.vo;

import lombok.Data;

/**
 * 获取长期令牌请求
 * 注意：请求体中的字段命名格式为SNAKE_CASE，需配置spring.jackson.property-naming-strategy=SNAKE_CASE
 *
 * @author Sud
 */
@Data
public class GetSSTokenReq {
    /**
     * 短期令牌，字段名：code
     */
    private String code;
}


