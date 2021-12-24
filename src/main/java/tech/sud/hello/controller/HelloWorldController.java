package tech.sud.hello.controller;

import tech.sud.hello.service.HelloWorldService;
import tech.sud.hello.vo.GetSSTokenReq;
import tech.sud.hello.vo.GetUserInfoReq;
import tech.sud.hello.vo.LoginReq;
import tech.sud.hello.vo.UpdateSSTokenReq;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Hello Controller
 *
 * @author Sud
 */
@RestController
public class HelloWorldController {

    @Resource
    private HelloWorldService helloWorldService;

    /**
     * 登录接口，获取针对当前用户(UID)的短期令牌Code
     * 调用方：接入端APP
     *
     * @param reqParam
     * @return
     */
    @PostMapping("/login")
    public Object hello(@RequestBody() LoginReq reqParam) {
        return helloWorldService.login(reqParam.getUserId());
    }

    /**
     * 短期令牌Code更换长期令牌SSToken
     * 调用方：游戏服务
     *
     * @param reqParam
     * @return
     */
    @PostMapping("/get_sstoken")
    public Object getSSToken(@RequestBody() GetSSTokenReq reqParam) {
        return helloWorldService.getSSToken(reqParam.getCode());
    }

    /**
     * 刷新长期令牌
     * 调用方：游戏服务
     *
     * @param reqParam
     * @return
     */
    @PostMapping("/update_sstoken")
    public Object updateSSToken(@RequestBody() UpdateSSTokenReq reqParam) {
        return helloWorldService.updateSSToken(reqParam.getSsToken());
    }

    /**
     * 获取用户信息
     * 调用方：游戏服务
     *
     * @param reqParam
     * @return
     */
    @PostMapping("/get_user_info")
    public Object getUserInfo(@RequestBody() GetUserInfoReq reqParam) {
        return helloWorldService.getUserInfo(reqParam.getSsToken());
    }

}
