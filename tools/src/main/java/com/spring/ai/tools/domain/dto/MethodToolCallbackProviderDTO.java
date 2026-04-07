package com.spring.ai.tools.domain.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 核心能力是扫描并自动将带有 @Tool 注解的方法转换为 ToolCallback
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/7
 */
@Data
@Builder
public class MethodToolCallbackProviderDTO {

    List<Object> tools;

}
