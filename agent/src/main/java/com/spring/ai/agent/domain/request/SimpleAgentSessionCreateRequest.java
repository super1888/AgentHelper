package com.spring.ai.agent.domain.request;

import lombok.Data;

/**
 * 会话创建请求。
 */
@Data
public class SimpleAgentSessionCreateRequest {

    /**
     * 指定绑定的版本号。
     *
     * <p>为空时优先绑定已发布版本，否则回退到当前最新草稿版本。</p>
     */
    private Integer versionNo;
}
