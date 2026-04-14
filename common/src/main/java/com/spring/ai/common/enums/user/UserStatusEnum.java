package com.spring.ai.common.enums.user;

import com.spring.ai.common.exception.EnumException;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态 1-启用, 0-禁用
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/14
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    ENABLE(1, "启用"),
    DISABLED(0, "禁用");
    /**
     * 编码
     */
    private final Integer code;
    /**
     * 描述
     */
    private final String desc;


    public static UserStatusEnum fromVal(Integer val) {
        return Stream.of(UserStatusEnum.values()).filter(userStatusEnum -> userStatusEnum.getCode().equals(val))
                .findFirst().orElseThrow(() -> new EnumException("未知的编码"));
    }

}
