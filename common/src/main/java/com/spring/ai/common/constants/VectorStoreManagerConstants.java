package com.spring.ai.common.constants;

/**
 * 向量库模块共享常量。
 * 统一定义元数据字段名，避免不同模块或不同类之间出现硬编码不一致。
 */
public final class VectorStoreManagerConstants {

    /**
     * 当前模块名称。
     */
    public static final String MODULE_NAME = "vectorStore";

    /**
     * 模块标识字段。
     */
    public static final String METADATA_MODULE = "knowledge_module";

    /**
     * 文档来源字段。
     */
    public static final String METADATA_SOURCE = "source";

    /**
     * 文件名字段。
     */
    public static final String METADATA_FILE_NAME = "file_name";

    /**
     * 文件扩展名字段。
     */
    public static final String METADATA_EXTENSION = "file_extension";

    /**
     * 文件内容类型字段。
     */
    public static final String METADATA_CONTENT_TYPE = "content_type";

    /**
     * 文件大小字段。
     */
    public static final String METADATA_FILE_SIZE = "file_size";

    /**
     * 上传时间字段。
     */
    public static final String METADATA_UPLOADED_AT = "uploaded_at";

    private VectorStoreManagerConstants() {
    }
}
