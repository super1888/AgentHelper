package com.spring.ai.interceptors.domain.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.chat.model.ChatModel;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
@Data
@Builder
public class ToolSelectionInterceptorDTO {

    /**
     * 【必填】用于工具选择的大语言模型 专门负责判断：用户问题需要调用哪些工具
     */
    private ChatModel selectionModel;

    /**
     * 【可选】工具选择系统提示词 默认：你的目标是选择最相关的工具来回答用户问题
     */
    private String systemPrompt;

    /**
     * 【可选】最多选择的工具数量 必须 > 0，不设置则不限制工具数量
     */
    private Integer maxTools;

    /**
     * 【可选】永远包含的工具名称集合 无论用户问什么，这些工具都会被保留
     */
    private Set<String> alwaysInclude;
}
