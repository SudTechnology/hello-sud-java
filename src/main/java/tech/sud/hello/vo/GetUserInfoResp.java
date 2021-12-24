package tech.sud.hello.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取用户信息响应
 *
 * @author Sud
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class GetUserInfoResp {

    /**
     * 用户ID
     */
    private String uid;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 头像地址（最优解为128*128）
     */
    private String avatarUrl;

    /**
     * 性别（男：male, 女：female）
     */
    private String gender;

}


