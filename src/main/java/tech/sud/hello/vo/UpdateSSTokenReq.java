package tech.sud.hello.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新长期令牌请求
 *
 * @author Sud
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class UpdateSSTokenReq {

    /**
     * 当前有效的长期令牌SSToken
     */
    private String ssToken;

}


