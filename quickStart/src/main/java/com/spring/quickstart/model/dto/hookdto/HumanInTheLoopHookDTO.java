package com.spring.quickstart.model.dto.hookdto;

import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
@Data
@Builder
public class HumanInTheLoopHookDTO {

    /**
     * 需要人工审批的工具配置
     * key：工具名（如 execute_sql、write_file）
     * value：工具审批配置（含审批描述）
     * 默认：空（无工具需要审批，直接执行）
     */
    private Map<String, ToolConfig> approvalOn;

    /**
     * 全局默认审批描述（当工具未单独配置时生效）
     * 默认："该操作需要人工审批确认"
     */
    private String defaultApprovalDesc;

    /**
     * 是否启用人工介入（true=启用，false=禁用）
     * 默认：true
     */
    private Boolean enabled;

}
