package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 短链接主表实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("short_link_record")
public class ShortLinkRecord extends BaseEntity {

    @TableField("short_code")
    private String shortCode;

    @TableField("short_url")
    private String shortUrl;

    @TableField("long_url")
    private String longUrl;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("domain")
    private String domain;

    @TableField("status")
    private String status;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("total_visit_count")
    private Long totalVisitCount;

    @TableField("unique_visitor_count")
    private Long uniqueVisitorCount;

    @TableField("unique_ip_count")
    private Long uniqueIpCount;

    @TableField("last_access_time")
    private LocalDateTime lastAccessTime;

    @TableField("deleted_flag")
    private Integer deletedFlag;
}
