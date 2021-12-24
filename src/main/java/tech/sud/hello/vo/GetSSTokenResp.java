package tech.sud.hello.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取长期令牌响应
 *
 * @author Sud
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class GetSSTokenResp {
    /**
     * 长期令牌SSToken
     */
    private String ssToken;
    /**
     * 长期令牌SSToken的过期时间（毫秒时间戳）
     */
    private long expireDate;

}


