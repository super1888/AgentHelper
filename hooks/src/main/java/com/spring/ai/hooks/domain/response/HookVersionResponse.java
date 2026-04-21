package com.spring.ai.hooks.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：Hook 版本响应
 */
@Data
@Builder
public class HookVersionResponse {

    private Long id;
    private Integer versionNo;
    private String versionCode;
    private String versionDescription;
    private String versionStatus;
    private String publishStatus;
    private String snapshotJson;
    private Long createTime;
}
