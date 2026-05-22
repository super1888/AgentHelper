package com.spring.ai.link.domain.request;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建短链接请求。
 */
@Data
public class ShortLinkCreateRequest {

    private String longUrl;

    private String title;

    private String description;

    private String customCode;

    private String domain;

    private LocalDateTime expireTime;
}
