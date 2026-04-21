package com.spring.ai.hooks.domain.request;

import java.util.List;
import lombok.Data;

/**
 * 文件用途：Hook 批量操作请求
 */
@Data
public class HookBatchActionRequest {

    private List<Long> hookIds;
    private String hookStatus;
    private String hookStage;
    private String riskLevel;
    private List<String> tagNames;
}
