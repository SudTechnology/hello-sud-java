package tech.sud.hello.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新长期令牌请求
 * 注意：请求体中的字段命名格式为SNAKE_CASE，需配置spring.jackson.property-naming-strategy=SNAKE_CASE
 *
 * @author Sud
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class UpdateSSTokenReq {

    /**
     * 当前有效的长期令牌，字段名：ss_token
     */
    private String ssToken;

}


