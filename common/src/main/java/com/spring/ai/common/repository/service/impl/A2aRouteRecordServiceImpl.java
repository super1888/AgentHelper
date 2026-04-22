package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.A2aRouteRecordMapper;
import com.spring.ai.common.repository.enitiy.A2aRouteRecord;
import com.spring.ai.common.repository.service.A2aRouteRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class A2aRouteRecordServiceImpl extends ServiceImpl<A2aRouteRecordMapper, A2aRouteRecord>
        implements A2aRouteRecordService {

    @Override
    public List<A2aRouteRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(A2aRouteRecord.class)
                .eq(A2aRouteRecord::getTenantId, tenantId)
                .orderByAsc(A2aRouteRecord::getPriorityNo)
                .orderByDesc(A2aRouteRecord::getUpdateTime)
                .orderByDesc(A2aRouteRecord::getId));
    }

    @Override
    public A2aRouteRecord getByRouteCode(Long tenantId, String routeCode) {
        if (tenantId == null || !StringUtils.hasText(routeCode)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(A2aRouteRecord.class)
                .eq(A2aRouteRecord::getTenantId, tenantId)
                .eq(A2aRouteRecord::getRouteCode, routeCode.trim())
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public A2aRouteRecord matchRoute(Long tenantId, String sourceAgentCode, String taskType) {
        String sourceCode = StringUtils.hasText(sourceAgentCode) ? sourceAgentCode.trim() : null;
        String normalizedTaskType = StringUtils.hasText(taskType) ? taskType.trim() : null;
        if (tenantId == null || !StringUtils.hasText(normalizedTaskType)) {
            return null;
        }
        if (StringUtils.hasText(sourceCode)) {
            A2aRouteRecord exactRoute = getOne(Wrappers.lambdaQuery(A2aRouteRecord.class)
                    .eq(A2aRouteRecord::getTenantId, tenantId)
                    .eq(A2aRouteRecord::getRouteStatus, "ENABLED")
                    .eq(A2aRouteRecord::getSourceAgentCode, sourceCode)
                    .eq(A2aRouteRecord::getTaskType, normalizedTaskType)
                    .orderByAsc(A2aRouteRecord::getPriorityNo)
                    .last(SqlConstants.LIMIT_ONE));
            if (exactRoute != null) {
                return exactRoute;
            }
        }
        return getOne(Wrappers.lambdaQuery(A2aRouteRecord.class)
                .eq(A2aRouteRecord::getTenantId, tenantId)
                .eq(A2aRouteRecord::getRouteStatus, "ENABLED")
                .isNull(A2aRouteRecord::getSourceAgentCode)
                .eq(A2aRouteRecord::getTaskType, normalizedTaskType)
                .orderByAsc(A2aRouteRecord::getPriorityNo)
                .last(SqlConstants.LIMIT_ONE));
    }
}
