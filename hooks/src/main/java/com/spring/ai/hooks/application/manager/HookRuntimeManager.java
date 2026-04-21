package com.spring.ai.hooks.application.manager;

import com.spring.ai.common.repository.enitiy.HookAgentBindingRecord;
import com.spring.ai.common.repository.enitiy.HookExecutionLogRecord;
import com.spring.ai.common.repository.enitiy.HookRecord;
import com.spring.ai.common.repository.service.HookAgentBindingRecordService;
import com.spring.ai.common.repository.service.HookExecutionLogRecordService;
import com.spring.ai.common.repository.service.HookRecordService;
import com.spring.ai.hooks.config.HookManagementConstants;
import com.spring.ai.hooks.domain.dto.HookRuntimeResultDTO;
import com.spring.ai.hooks.domain.dto.HookSnapshotDTO;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件用途：Hook 运行时执行管理器
 * 核心职责：在真实 Agent 会话中装配并执行已发布 Hook
 */
@Component
public class HookRuntimeManager {

    @Resource
    private HookRecordService hookRecordService;

    @Resource
    private HookAgentBindingRecordService hookAgentBindingRecordService;

    @Resource
    private HookExecutionLogRecordService hookExecutionLogRecordService;

    @Resource
    private HookSupportManager hookSupportManager;

    /**
     * 执行请求前 Hook。
     */
    public HookRuntimeResultDTO applyPreModelHooks(Long tenantId, String agentCode, String sessionCode, String requestMessage) {
        return applyHooks(tenantId, agentCode, sessionCode, null, "PRE_MODEL", requestMessage, null);
    }

    /**
     * 执行响应后 Hook。
     */
    public HookRuntimeResultDTO applyPostModelHooks(
            Long tenantId,
            String agentCode,
            String sessionCode,
            List<String> selectedHookCodes,
            String requestMessage,
            String responseMessage
    ) {
        return applyHooks(tenantId, agentCode, sessionCode, selectedHookCodes, "POST_MODEL", responseMessage, requestMessage);
    }

    /**
     * 执行请求前 Hook，并允许按版本快照限制 Hook 范围。
     */
    public HookRuntimeResultDTO applyPreModelHooks(
            Long tenantId,
            String agentCode,
            String sessionCode,
            List<String> selectedHookCodes,
            String requestMessage
    ) {
        return applyHooks(tenantId, agentCode, sessionCode, selectedHookCodes, "PRE_MODEL", requestMessage, null);
    }

    private HookRuntimeResultDTO applyHooks(
            Long tenantId,
            String agentCode,
            String sessionCode,
            List<String> selectedHookCodes,
            String stage,
            String content,
            String requestMessage
    ) {
        String finalContent = defaultText(content);
        int matchedCount = 0;
        for (HookRecord record : hookRecordService.listByTenantId(tenantId)) {
            if (!isRuntimeEnabled(record, stage)) {
                continue;
            }
            if (!matchSelectedHooks(selectedHookCodes, record.getHookCode())) {
                continue;
            }
            if (!matchAgent(record.getId(), tenantId, agentCode)) {
                continue;
            }
            matchedCount++;
            HookSnapshotDTO snapshot = hookSupportManager.parseSnapshot(record.getExt());
            RuntimeHandleResult handleResult = handleContent(record, snapshot, stage, finalContent);
            saveRuntimeLog(record, tenantId, agentCode, sessionCode, requestMessage, finalContent, handleResult);
            if (isBlocked(handleResult)) {
                return HookRuntimeResultDTO.builder()
                        .content(finalContent)
                        .blocked(1)
                        .failureReason(handleResult.failureReason())
                        .matchedHookCount(matchedCount)
                        .build();
            }
            finalContent = handleResult.content();
        }
        return HookRuntimeResultDTO.builder()
                .content(finalContent)
                .blocked(0)
                .failureReason(null)
                .matchedHookCount(matchedCount)
                .build();
    }

    private boolean isRuntimeEnabled(HookRecord record, String stage) {
        return HookManagementConstants.PUBLISH_STATUS_PUBLISHED.equals(record.getPublishStatus())
                && HookManagementConstants.HOOK_STATUS_ENABLED.equals(record.getHookStatus())
                && stage.equals(record.getHookStage());
    }

    private boolean matchSelectedHooks(List<String> selectedHookCodes, String hookCode) {
        if (selectedHookCodes == null || selectedHookCodes.isEmpty()) {
            return true;
        }
        return selectedHookCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .anyMatch(code -> code.equals(hookCode));
    }

    private boolean matchAgent(Long hookId, Long tenantId, String agentCode) {
        List<HookAgentBindingRecord> bindings = hookAgentBindingRecordService.listByHookId(hookId, tenantId);
        if (bindings.isEmpty()) {
            return true;
        }
        return bindings.stream()
                .filter(item -> Integer.valueOf(1).equals(item.getEnabled()))
                .anyMatch(item -> !StringUtils.hasText(item.getTargetAgentCode()) || item.getTargetAgentCode().trim().equals(agentCode));
    }

    private RuntimeHandleResult handleContent(HookRecord record, HookSnapshotDTO snapshot, String stage, String originalContent) {
        String content = defaultText(originalContent);
        if ("PRE_MODEL".equals(stage)) {
            content = prependPrompt(snapshot, content);
            content = maskBySecurity(snapshot, content);
            String blockedKeyword = matchBlockedKeyword(snapshot, content);
            if (blockedKeyword != null && "BLOCK".equals(record.getFailStrategy())) {
                return new RuntimeHandleResult(content, 1, "blocked by hook keyword: " + blockedKeyword);
            }
        }
        if ("POST_MODEL".equals(stage)) {
            content = maskForbiddenOutputs(snapshot, content);
            content = appendDisclaimer(snapshot, content);
        }
        return new RuntimeHandleResult(content, 0, null);
    }

    private String prependPrompt(HookSnapshotDTO snapshot, String content) {
        Map<String, Object> runtimeConfig = snapshot.getRuntimeConfig();
        if (runtimeConfig == null) {
            return content;
        }
        Object prependPrompt = runtimeConfig.get("prependPrompt");
        if (prependPrompt == null || !StringUtils.hasText(String.valueOf(prependPrompt))) {
            return content;
        }
        return String.valueOf(prependPrompt).trim() + "\n" + content;
    }

    private String appendDisclaimer(HookSnapshotDTO snapshot, String content) {
        Map<String, Object> degradationConfig = snapshot.getDegradationConfig();
        if (degradationConfig == null) {
            return content;
        }
        Object appendDisclaimer = degradationConfig.get("appendDisclaimer");
        if (appendDisclaimer == null || !StringUtils.hasText(String.valueOf(appendDisclaimer))) {
            return content;
        }
        return content + "\n" + String.valueOf(appendDisclaimer).trim();
    }

    private String maskBySecurity(HookSnapshotDTO snapshot, String content) {
        Map<String, Object> securityConfig = snapshot.getSecurityConfig();
        if (securityConfig == null) {
            return content;
        }
        Object maskPatterns = securityConfig.get("maskPatterns");
        if (!(maskPatterns instanceof List<?> patternList)) {
            return content;
        }
        String result = content;
        for (Object item : patternList) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result = result.replaceAll(Pattern.quote(String.valueOf(item)), "***");
            }
        }
        return result;
    }

    private String maskForbiddenOutputs(HookSnapshotDTO snapshot, String content) {
        Map<String, Object> securityConfig = snapshot.getSecurityConfig();
        if (securityConfig == null) {
            return content;
        }
        Object forbiddenOutputs = securityConfig.get("forbiddenOutputs");
        if (!(forbiddenOutputs instanceof List<?> outputList)) {
            return content;
        }
        String result = content;
        for (Object item : outputList) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result = result.replace(String.valueOf(item), "[filtered]");
            }
        }
        return result;
    }

    private String matchBlockedKeyword(HookSnapshotDTO snapshot, String content) {
        Map<String, Object> conditionConfig = snapshot.getConditionConfig();
        if (conditionConfig == null) {
            return null;
        }
        Object blockedKeywords = conditionConfig.get("blockedKeywords");
        if (!(blockedKeywords instanceof List<?> keywordList)) {
            return null;
        }
        for (Object item : keywordList) {
            if (item != null && StringUtils.hasText(String.valueOf(item)) && content.contains(String.valueOf(item))) {
                return String.valueOf(item);
            }
        }
        return null;
    }

    private void saveRuntimeLog(
            HookRecord record,
            Long tenantId,
            String agentCode,
            String sessionCode,
            String requestMessage,
            String responseMessage,
            RuntimeHandleResult handleResult
    ) {
        HookExecutionLogRecord log = new HookExecutionLogRecord();
        log.setHookId(record.getId());
        log.setHookCode(record.getHookCode());
        log.setHookName(record.getHookName());
        log.setTenantId(tenantId);
        log.setSourceType(HookManagementConstants.SOURCE_TYPE_RUNTIME);
        log.setTraceId(UUID.randomUUID().toString());
        log.setAgentCode(agentCode);
        log.setSessionCode(sessionCode);
        log.setRequestPayloadJson(requestMessage);
        log.setContextPayloadJson(null);
        log.setResponsePayloadJson(responseMessage);
        log.setExecuteStatus(isBlocked(handleResult)
                ? HookManagementConstants.EXECUTE_STATUS_FAILED
                : HookManagementConstants.EXECUTE_STATUS_SUCCESS);
        log.setSuccessFlag(isBlocked(handleResult) ? 0 : 1);
        log.setElapsedMs(1L);
        log.setFailureReason(handleResult.failureReason());
        hookExecutionLogRecordService.save(log);
    }

    private boolean isBlocked(RuntimeHandleResult handleResult) {
        return handleResult != null && Integer.valueOf(1).equals(handleResult.blocked());
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private record RuntimeHandleResult(String content, Integer blocked, String failureReason) {
    }
}
