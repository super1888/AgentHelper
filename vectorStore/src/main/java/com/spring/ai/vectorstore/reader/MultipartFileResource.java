package com.spring.ai.vectorstore.reader;

import org.springframework.core.io.ByteArrayResource;

/**
 * 保留原始文件名的字节数组资源。
 * 主要用于第三方文档读取器需要访问文件名信息的场景。
 */
public class MultipartFileResource extends ByteArrayResource {

    private final String filename;

    public MultipartFileResource(byte[] byteArray, String filename) {
        super(byteArray);
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
