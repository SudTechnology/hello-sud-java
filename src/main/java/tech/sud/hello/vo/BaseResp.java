package tech.sud.hello.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("ret_code")
    private int retCode;

    /**
     * 响应消息
     */
    @JsonProperty("ret_msg")
    private String retMsg;

    /**
     * 业务响应数据
     */
    @JsonProperty("data")
    private T data;

    /**
     * sdk错误码
     */
    @JsonProperty("sdk_error_code")
    private Integer sdkErrorCode;


    public void setRetCode(RetCodeEnum retCode) {
        this.retCode = retCode.getIndex();
        this.retMsg = retCode.getName();
    }

}
