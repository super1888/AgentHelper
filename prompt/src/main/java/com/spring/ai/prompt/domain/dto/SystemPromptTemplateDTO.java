package com.spring.ai.prompt.domain.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.core.io.Resource;

/**
 * SystemPromptTemplate 构建入参 DTO  用途：封装系统提示词模板的所有配置参数
 *  SystemPrompt 和 Instruction 的区别
 * 特性	SystemPrompt	Instruction
 * 作用位置	系统消息（SystemMessage）	用户消息（UserMessage）
 * 用途	定义路由Agent的角色、职责和决策规则	提供具体的路由指导或额外上下文
 * 优先级	更高，影响整体路由行为	作为补充信息
 * 使用场景	需要详细定义路由规则和Agent职责时	需要提供特定场景的路由指导时
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/3
 */
@Data
@Builder
public class SystemPromptTemplateDTO {

    /**
     * 提示词模板字符串（直接传入文本） 作用：直接使用字符串作为 AI 系统提示词模板 注意：与 resource 二选一，不能同时传
     */
    private String template;

    /**
     * 提示词模板资源文件（如 .txt / .vm / .ftl 模板文件） 作用：从文件/资源中读取提示词模板（支持 classpath、文件路径、URL 等） 注意：与 template 二选一，不能同时传
     */
    private Resource resource;

    /**
     * 模板变量（动态参数） 作用：模板中可使用 {key} 占位符，通过 map 传入变量值进行渲染 例如：模板中写 {username}，这里传入 Map.of("username", "张三")
     */
    private Map<String, Object> variables;

    /**
     * 模板渲染器 作用：自定义模板渲染逻辑（如使用 FreeMarker、Velocity 等） 默认：Spring AI 内置的默认渲染器，不传则使用默认
     */
    private TemplateRenderer renderer;

}
