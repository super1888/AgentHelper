package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 向量文件管理台账实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vector_store_file")
public class VectorStoreFileRecord extends BaseEntity {

    @TableField("module_name")
    private String moduleName;

    @TableField("file_name")
    private String fileName;

    @TableField("file_extension")
    private String fileExtension;

    @TableField("content_type")
    private String contentType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("source_document_count")
    private Integer sourceDocumentCount;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("uploaded_at")
    private String uploadedAt;

    @TableField("store_status")
    private String storeStatus;

    @TableField("last_operation_message")
    private String lastOperationMessage;
}
