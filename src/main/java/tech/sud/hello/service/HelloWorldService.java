package tech.sud.hello.service;

import tech.sud.hello.RetCodeEnum;
import tech.sud.hello.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.sud.mgp.auth.api.SudCode;
import tech.sud.mgp.auth.api.SudMGPAuth;
import tech.sud.mgp.auth.api.SudSSToken;
import tech.sud.mgp.auth.api.SudUid;

import javax.annotation.Resource;

/**
 * Hello Service
 *
 * @author Sud
 */
@Slf4j
@Service
public class HelloWorldService {

    @Resource
    private SudMGPAuth sudMGPAuth;

    /**
     * 登录（GetCode）
     *
     * @param userId 用户ID
     * @return 登录响应
     */
    public BaseResp<LoginResp> login(String userId) {
        BaseResp<LoginResp> baseResp = new BaseResp<>();

        // 生成短期令牌Code, 不传expireDuration参数则使用SDK中默认两小时过期
        SudCode sudCode = sudMGPAuth.getCode(userId);

        // 返回响应结果
        baseResp.setData(LoginResp.builder()
                .code(sudCode.getCode())
                .expireDate(sudCode.getExpireDate())
                .build());
        return baseResp;
    }

    /**
     * 获取长期令牌SSToken(根据短期令牌Code更换长期令牌SSToken)
     *
     * @param code 短期令牌
     * @return 长期令牌SSToken响应
     */
    public BaseResp<GetSSTokenResp> getSSToken(String code) {
        BaseResp<GetSSTokenResp> baseResp = new BaseResp<>();

        SudUid sudUid = sudMGPAuth.getUidByCode(code);
        if (!sudUid.isSuccess()) {
            baseResp.setRetCode(RetCodeEnum.REQUEST_FAILED);
            baseResp.setSdkErrorCode(sudUid.getErrorCode());
            return baseResp;
        }
        SudSSToken sudSSToken = sudMGPAuth.getSSToken(sudUid.getUid());
        baseResp.setData(GetSSTokenResp.builder()
                .ssToken(sudSSToken.getToken())
                .expireDate(sudSSToken.getExpireDate())
                .build());
        return baseResp;
    }

    /**
     * 更新长期令牌SSToken(根据当前的长期令牌SSToken更换新的SSToken)
     *
     * @param ssToken 长期令牌
     * @return 长期令牌SSToken响应
     */
    public BaseResp<UpdateSSTokenResp> updateSSToken(String ssToken) {
        BaseResp<UpdateSSTokenResp> baseResp = new BaseResp<>();
        SudUid sudUid = sudMGPAuth.getUidBySSToken(ssToken);
        if (!sudUid.isSuccess()) {
            baseResp.setRetCode(RetCodeEnum.REQUEST_FAILED);
            baseResp.setSdkErrorCode(sudUid.getErrorCode());
            return baseResp;
        }

        SudSSToken sudSSToken = sudMGPAuth.getSSToken(sudUid.getUid());
        baseResp.setData(UpdateSSTokenResp.builder()
                .ssToken(sudSSToken.getToken())
                .expireDate(sudSSToken.getExpireDate())
                .build());
        return baseResp;
    }

    /**
     * 获取用户信息
     *
     * @param ssToken 长期令牌SSToken
     * @return 用户信息响应
     */
    public BaseResp<GetUserInfoResp> getUserInfo(String ssToken) {
        BaseResp<GetUserInfoResp> baseResp = new BaseResp<>();

        SudUid sudUid = sudMGPAuth.getUidBySSToken(ssToken);
        if (!sudUid.isSuccess()) {
            baseResp.setRetCode(RetCodeEnum.REQUEST_FAILED);
            baseResp.setSdkErrorCode(sudUid.getErrorCode());
            return baseResp;
        }
        // TODO：需获取uid对应的用户信息（昵称，性别，头像地址），头像地址最优为返回128*128
        baseResp.setData(GetUserInfoResp.builder()
                .uid(sudUid.getUid())
                .nickName("HELLO-SUD")
                .gender("male")
                .avatarUrl("https://ATATAR-URL")
                .build());
        return baseResp;
    }


}
