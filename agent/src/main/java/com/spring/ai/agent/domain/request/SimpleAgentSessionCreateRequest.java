package com.spring.ai.agent.domain.request;

import lombok.Data;

/**
 * 创建会话请求。
 */
@Data
public class SimpleAgentSessionCreateRequest {

    /**
     * 指定要绑定的版本号。
     *
     * <p>为空时优先使用已发布版本，没有发布版本时回退到最新版本。</p>
     */
    private Integer versionNo;
}
