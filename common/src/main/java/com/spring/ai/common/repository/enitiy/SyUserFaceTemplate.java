package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户人脸模板表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sy_user_face_template")
public class SyUserFaceTemplate extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("face_template_code")
    private String faceTemplateCode;

    @TableField("embedding_cipher_text")
    private String embeddingCipherText;

    @TableField("embedding_dimension")
    private Integer embeddingDimension;

    @TableField("embedding_version")
    private String embeddingVersion;

    @TableField("quality_score")
    private Double qualityScore;

    @TableField("liveness_score")
    private Double livenessScore;

    @TableField("source_image_url")
    private String sourceImageUrl;

    @TableField("image_sha256")
    private String imageSha256;

    @TableField("status")
    private String status;

    @TableField("last_verified_time")
    private java.time.LocalDateTime lastVerifiedTime;
}
