package com.spring.ai.common.enums.user;

import com.spring.ai.common.exception.EnumException;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举。
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    ENABLE(1, "启用"),
    DISABLED(0, "禁用");

    /**
     * 状态编码。
     */
    private final Integer code;

    /**
     * 状态描述。
     */
    private final String desc;

    /**
     * 按编码解析枚举。
     *
     * @param val 状态编码
     * @return 用户状态枚举
     */
    public static UserStatusEnum fromVal(Integer val) {
        return Stream.of(UserStatusEnum.values())
                .filter(userStatusEnum -> userStatusEnum.getCode().equals(val))
                .findFirst()
                .orElseThrow(() -> new EnumException("未知的用户状态编码"));
    }
}
