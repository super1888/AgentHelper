package com.spring.ai.agent.domain.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SequentialAgent 数据传输对象 用于封装创建SequentialAgent所需的所有参数，统一参数传递
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class SequentialAgentDTO extends FlowAgentDTO{



}
