package tech.sud.hello.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应
 * 注意：响应体中的字段命名格式为SNAKE_CASE，需配置spring.jackson.property-naming-strategy=SNAKE_CASE
 *
 * @author Sud
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class GetCodeResp {
    /**
     * 短期令牌，字段名：code
     */
    @JsonProperty("code")
    private String code;

    /**
     * 短期令牌Code的过期时间（毫秒时间戳），字段名：expire_date
     */
    @JsonProperty("expire_date")
    private long expireDate;
}


