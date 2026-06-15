package com.spring.ai.bigfile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BigFileProperties {

    @Value("${app.big-file.storage-root:${user.dir}/data/bigfile}")
    private String storageRoot;

    @Value("${app.big-file.max-file-size:2147483648}")
    private long maxFileSize;

    @Value("${app.big-file.default-chunk-size:5242880}")
    private long defaultChunkSize;

    public String getStorageRoot() {
        return storageRoot;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public long getDefaultChunkSize() {
        return defaultChunkSize;
    }
}
