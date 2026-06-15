package com.spring.ai.codehelper.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码助手会话事件实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("code_helper_session_event")
public class CodeHelperSessionEventRecord extends BaseEntity {

    @TableField("session_code")
    private String sessionCode;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("event_sequence")
    private Long eventSequence;

    @TableField("event_role")
    private String eventRole;

    @TableField("event_content")
    private String eventContent;
}
