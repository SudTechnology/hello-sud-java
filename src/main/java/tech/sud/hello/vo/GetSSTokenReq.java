package tech.sud.hello.vo;

import lombok.Data;

/**
 * 获取长期令牌请求
 *
 * @author Sud
 */
@Data
public class GetSSTokenReq {
    /**
     * 短期令牌Code
     */
    private String code;
}


