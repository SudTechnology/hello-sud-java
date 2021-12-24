package tech.sud.hello.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求
 *
 * @author Sud
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class LoginReq {
    /**
     * 用户ID（必填参数）
     */
    String userId;

    /**
     * TODO：其他参数视接入方业务决定
     */
}
