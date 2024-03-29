package tech.sud.hello.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取长期令牌响应
 * 注意：响应体中的字段命名格式为SNAKE_CASE，需配置spring.jackson.property-naming-strategy=SNAKE_CASE
 *
 * @author Sud
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class GetSSTokenResp {
    /**
     * 长期令牌，字段名:ss_token
     */
    @JsonProperty("ss_token")
    private String ssToken;

    /**
     * 长期令牌SSToken的过期时间（毫秒时间戳），字段名:expire_date
     */
    @JsonProperty("expire_date")
    private long expireDate;

    /**
     * 用户信息，字段名:user_info
     */
    @JsonProperty("user_info")
    private GetUserInfoResp userInfo;

}


