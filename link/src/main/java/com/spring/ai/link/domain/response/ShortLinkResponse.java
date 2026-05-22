package com.spring.ai.link.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短链接信息响应。
 */
@Data
public class ShortLinkResponse {

    private Long id;

    private String shortCode;

    private String shortUrl;

    private String longUrl;

    private String title;

    private String description;

    private String domain;

    private String status;

    private LocalDateTime expireTime;

    private Long totalVisitCount;

    private Long uniqueVisitorCount;

    private Long uniqueIpCount;

    private LocalDateTime lastAccessTime;

    private LocalDateTime createTime;
}
