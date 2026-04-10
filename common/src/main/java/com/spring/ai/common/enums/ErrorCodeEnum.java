package com.spring.ai.common.enums;

/**
 * 全局错误码枚举。
 *
 * <p>用于在不同模块之间统一错误码语义，避免每个模块各自定义一套错误返回结构。</p>
 */
public enum ErrorCodeEnum {

    SUCCESS("00000", "操作成功"),
    BAD_REQUEST("40000", "请求参数有误"),
    NOT_FOUND("40400", "请求资源不存在"),
    AGENT_CONFIG_ERROR("A1000", "Agent 配置异常"),
    TOOL_EXECUTION_ERROR("T1000", "工具执行异常"),
    VECTOR_STORE_ERROR("V1000", "向量存储处理异常"),
    INTERNAL_SERVER_ERROR("50000", "系统内部异常");

    private final String code;
    private final String message;

    ErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
