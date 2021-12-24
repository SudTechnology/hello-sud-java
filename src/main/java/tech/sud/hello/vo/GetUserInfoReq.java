package tech.sud.hello.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取用户信息请求
 *
 * @author Sud
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class GetUserInfoReq {

    private String ssToken;
}


