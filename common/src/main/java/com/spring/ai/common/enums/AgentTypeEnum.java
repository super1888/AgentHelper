package com.spring.ai.common.enums;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
public enum AgentTypeEnum {

    DEFAULT("0", "默认"),
    REACT("1", "普通Agent"),
    SEQUENTIAL("2", "工作流顺序执行Agent"),
    PARALLEL_AGENT("3", "工作流并行执行Agent"),
    LLM_ROUTING_AGENT("4", "动态决定将请求路由到哪个子Agent"),
    SUPERVISOR_AGENT("5", "支持多步骤循环路由Agent"),
    CUSTOMIZED_AGENT("6", "自定义执行顺序Agent"),
    ;

    private String code;
    private String desc;

    AgentTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }


}
