package com.spring.ai.hooks.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：Hook 绑定响应
 */
@Data
@Builder
public class HookBindingResponse {

    private Long id;
    private Long hookId;
    private String hookCode;
    private String bindingName;
    private String bindingScope;
    private String targetAgentCode;
    private String targetModelCode;
    private String environmentCode;
    private Integer priorityNo;
    private Integer enabled;
    private String remark;
    private Long createTime;
}
