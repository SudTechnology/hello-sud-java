package tech.sud.hello.vo;

import tech.sud.hello.RetCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 基础响应体
 *
 * @author Sud
 * @param <T>
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class BaseResp<T> {

    /**
     * 响应码
     */
    private int retCode;

    /**
     * 响应消息
     */
    private String retMsg;

    /**
     * 业务响应数据
     */
    private T data;

    /**
     * sdk错误码
     */
    private Integer sdkErrorCode;


    public void setRetCode(RetCodeEnum retCode) {
        this.retCode = retCode.getIndex();
        this.retMsg = retCode.getName();
    }

}
