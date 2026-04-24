package com.spring.ai.interceptors.application.manager;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.InterceptorRecord;
import com.spring.ai.common.repository.service.InterceptorAgentBindingRecordService;
import com.spring.ai.common.repository.service.InterceptorExecutionLogRecordService;
import com.spring.ai.common.repository.service.InterceptorRecordService;
import com.spring.ai.common.repository.service.InterceptorTestCaseRecordService;
import com.spring.ai.common.utils.CommonJsonUtils;
import com.spring.ai.common.utils.CommonTextUtils;
import com.spring.ai.common.web.CurrentUserContextSupport;
import com.spring.ai.interceptors.domain.dto.InterceptorSnapshotDTO;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Support utilities for interceptor management.
 */
@Component
public class InterceptorSupportManager {

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private InterceptorRecordService interceptorRecordService;

    @Resource
    private InterceptorAgentBindingRecordService interceptorAgentBindingRecordService;

    @Resource
    private InterceptorTestCaseRecordService interceptorTestCaseRecordService;

    @Resource
    private InterceptorExecutionLogRecordService interceptorExecutionLogRecordService;

    @Resource
    private CommonJsonUtils commonJsonUtils;

    public Long getCurrentUserId() {
        return currentUserContextSupport.getCurrentUserId();
    }

    public String getCurrentUserName() {
        return currentUserContextSupport.getCurrentUserName();
    }

    public Long getCurrentTenantId() {
        return currentUserContextSupport.getCurrentTenantIdWithAutoInit();
    }

    public InterceptorRecord requireInterceptor(Long interceptorId) {
        InterceptorRecord record = interceptorRecordService.getById(interceptorId);
        if (record == null || !Objects.equals(record.getTenantId(), getCurrentTenantId()) || Integer.valueOf(1).equals(record.getDeletedFlag())) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "Interceptor not found: " + interceptorId);
        }
        return record;
    }

    public InterceptorSnapshotDTO parseSnapshot(String json) {
        InterceptorSnapshotDTO snapshot = commonJsonUtils.parseObject(json, InterceptorSnapshotDTO.class);
        return snapshot == null ? InterceptorSnapshotDTO.builder().build() : snapshot;
    }

    public Map<String, Object> parseMap(String json) {
        return commonJsonUtils.parseMap(json);
    }

    public String toJson(Object value) {
        return commonJsonUtils.toJson(value);
    }

    public String prettyJson(Object value) {
        return commonJsonUtils.prettyJson(value);
    }

    public Integer countBindings(Long interceptorId) {
        return interceptorAgentBindingRecordService.listByInterceptorId(interceptorId, getCurrentTenantId()).size();
    }

    public Integer countTestCases(Long interceptorId) {
        return interceptorTestCaseRecordService.listByInterceptorId(interceptorId, getCurrentTenantId()).size();
    }

    public Integer countLogs(Long interceptorId) {
        return interceptorExecutionLogRecordService.listByCondition(getCurrentTenantId(), interceptorId, null, null).size();
    }

    /**
     * 将任意对象安全转换为对象列表，避免模拟链路里反复做 instanceof 判断。
     */
    public List<Object> objectList(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        return List.of();
    }

    /**
     * 返回第一个非空值，用于兼容请求参数和上下文参数的双来源读取。
     */
    public Object firstNonNull(Object left, Object right) {
        return left == null ? right : left;
    }

    /**
     * 将配置值解析为整数，解析失败时回退默认值，避免模拟配置把调试流程打断。
     */
    public int numberValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 向字符串列表追加值时自动裁剪空白并去重。
     */
    public void addIfAbsent(List<String> values, String value) {
        String normalizedValue = CommonTextUtils.trimToNull(value);
        if (normalizedValue != null && !values.contains(normalizedValue)) {
            values.add(normalizedValue);
        }
    }

    /**
     * 构造按插入顺序输出的 Map，便于调试响应稳定展示。
     */
    public Map<String, Object> orderedMap(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
