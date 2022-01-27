package tech.sud.hello;

import lombok.Getter;
import lombok.Setter;

/**
 * 返回码枚举定义
 *
 * @author Sud
 */
public enum RetCodeEnum {
    /**
     * 成功
     */
    SUCCESS(0, "成功"),

    /**
     * 失败
     */
    REQUEST_FAILED(1, "失败");

    @Getter
    private final String name;

    @Getter
    private final int index;

    RetCodeEnum(int index, String name) {
        this.name = name;
        this.index = index;
    }


}
