package com.spring.ai.hooks.domain.request;

import lombok.Data;

/**
 * 文件用途：Hook 绑定保存请求
 */
@Data
public class HookBindingSaveRequest {

    private String bindingName;
    private String bindingScope;
    private String targetAgentCode;
    private String targetModelCode;
    private String environmentCode;
    private Integer priorityNo;
    private Integer enabled;
    private String remark;
}
