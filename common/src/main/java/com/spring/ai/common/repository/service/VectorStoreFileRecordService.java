package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.VectorStoreFileRecord;
import java.util.List;

/**
 * 向量文件台账仓储服务
 */
public interface VectorStoreFileRecordService extends IService<VectorStoreFileRecord> {

    VectorStoreFileRecord getByModuleAndFileName(String moduleName, String fileName);

    List<VectorStoreFileRecord> listByModule(String moduleName);

    long countByModuleAndStatus(String moduleName, String storeStatus);
}
