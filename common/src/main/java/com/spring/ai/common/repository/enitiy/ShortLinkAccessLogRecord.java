package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 短链接访问日志实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("short_link_access_log_record")
public class ShortLinkAccessLogRecord extends BaseEntity {

    @TableField("short_code")
    private String shortCode;

    @TableField("long_url")
    private String longUrl;

    @TableField("visitor_id")
    private String visitorId;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("user_agent")
    private String userAgent;

    @TableField("referer")
    private String referer;

    @TableField("access_time")
    private LocalDateTime accessTime;

    @TableField("success_flag")
    private Integer successFlag;

    @TableField("fail_reason")
    private String failReason;
}
