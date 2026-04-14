package com.spring.ai.common.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公共基础实体。
 * 当前约定：
 * 1. 主键使用业务层生成的雪花 ID
 * 2. deleteFlag 使用 MyBatis-Plus 逻辑删除
 * 3. createTime 和 updateTime 预留给后续 MetaObjectHandler 自动填充
 */
@Data
public abstract class BaseEntity implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("create_by")
    private Long createBy;

    @TableField("update_by")
    private Long updateBy;

    @TableLogic(value = "0", delval = "1")
    @TableField("delete_flag")
    private Integer deleteFlag;

    @Version
    @TableField("version")
    private Integer version;
}
