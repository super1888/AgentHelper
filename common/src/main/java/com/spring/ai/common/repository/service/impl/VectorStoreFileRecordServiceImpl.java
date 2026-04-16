package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.VectorStoreFileRecordMapper;
import com.spring.ai.common.repository.enitiy.VectorStoreFileRecord;
import com.spring.ai.common.repository.service.VectorStoreFileRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 向量文件台账仓储服务实现
 */
@Service
public class VectorStoreFileRecordServiceImpl extends ServiceImpl<VectorStoreFileRecordMapper, VectorStoreFileRecord>
        implements VectorStoreFileRecordService {

    @Override
    public VectorStoreFileRecord getByModuleAndFileName(String moduleName, String fileName) {
        if (!StringUtils.hasText(moduleName) || !StringUtils.hasText(fileName)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(VectorStoreFileRecord.class)
                .eq(VectorStoreFileRecord::getModuleName, moduleName.trim())
                .eq(VectorStoreFileRecord::getFileName, fileName.trim())
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public List<VectorStoreFileRecord> listByModule(String moduleName) {
        if (!StringUtils.hasText(moduleName)) {
            return List.of();
        }
        return list(Wrappers.lambdaQuery(VectorStoreFileRecord.class)
                .eq(VectorStoreFileRecord::getModuleName, moduleName.trim())
                .orderByDesc(VectorStoreFileRecord::getUpdateTime)
                .orderByDesc(VectorStoreFileRecord::getId));
    }

    @Override
    public long countByModuleAndStatus(String moduleName, String storeStatus) {
        if (!StringUtils.hasText(moduleName) || !StringUtils.hasText(storeStatus)) {
            return 0L;
        }
        return count(Wrappers.lambdaQuery(VectorStoreFileRecord.class)
                .eq(VectorStoreFileRecord::getModuleName, moduleName.trim())
                .eq(VectorStoreFileRecord::getStoreStatus, storeStatus.trim()));
    }
}
