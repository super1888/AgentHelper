package com.spring.ai.hooks.domain.request;

import lombok.Data;

/**
 * 文件用途：Hook 版本回滚请求
 */
@Data
public class HookVersionRollbackRequest {

    private Integer targetVersionNo;
    private String versionDescription;
}
