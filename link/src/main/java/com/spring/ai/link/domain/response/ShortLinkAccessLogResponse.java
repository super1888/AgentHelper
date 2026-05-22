package com.spring.ai.link.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短链接访问明细响应。
 */
@Data
public class ShortLinkAccessLogResponse {

    private String shortCode;

    private String visitorId;

    private String ipAddress;

    private String userAgent;

    private String referer;

    private LocalDateTime accessTime;

    private Integer successFlag;

    private String failReason;
}
