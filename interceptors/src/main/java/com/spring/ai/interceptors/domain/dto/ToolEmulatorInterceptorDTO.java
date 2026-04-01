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
public class ToolEmulatorInterceptorDTO {

    /**
     * 【必填】用于模拟工具返回的 AI 对话模型 负责根据工具名、描述、入参生成仿真的工具返回结果
     */
    private ChatModel emulatorModel;

    /**
     * 【可选】需要模拟的工具名称集合 仅当 emulateAll = false 时生效 只模拟这里指定的工具，其余真实调用
     */
    private Set<String> toolsToEmulate;

    /**
     * 【可选】是否模拟所有工具 true：Agent 调用的所有工具都走 AI 模拟 false：仅模拟 toolsToEmulate 中指定的工具
     */
    private boolean emulateAll;

    /**
     * 【可选】AI 模拟工具的提示词模板 占位符顺序：%s = 工具名、%s = 工具描述、%s = 调用参数 要求返回：仅返回工具结果，无多余描述
     */
    private String promptTemplate;


}
