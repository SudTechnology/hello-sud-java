package tech.sud.hello.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应
 *
 * @author Sud
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class LoginResp {
    /**
     * 短期令牌Code
     */
    private String code;

    /**
     * 过期时间戳
     */
    private long expireDate;
}


