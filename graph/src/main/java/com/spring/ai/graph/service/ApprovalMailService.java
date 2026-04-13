package com.spring.ai.graph.service;

import com.spring.ai.common.utils.EmailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 审批邮件服务。
 *
 * <p>这里没有重复造一套发邮件底层能力，而是直接复用你系统里已经存在的 {@link EmailUtil}。
 * 这样这套 Graph 示例就真正接入了你现有框架，而不是单独写一个假实现。</p>
 */
@Slf4j
@Service
public class ApprovalMailService {

    /**
     * 发送审批推送邮件。
     *
     * @return 返回可直接落到 Graph 状态中的发送结果，避免节点层再做二次包装
     */
    public String sendApprovalPush(String approverEmail, String subject, String content) {
        return send(approverEmail, subject, content, "审批推送");
    }

    /**
     * 发送审批结果通知邮件。
     */
    public String sendApprovalResult(String applicantEmail, String subject, String content) {
        return send(applicantEmail, subject, content, "审批结果通知");
    }

    private String send(String targetEmail, String subject, String content, String scene) {
        try {
            EmailUtil.sendTextMail(targetEmail, subject, content);
            log.info("{}邮件发送成功, targetEmail={}, subject={}", scene, targetEmail, subject);
            return "SUCCESS";
        } catch (Exception exception) {
            log.error("{}邮件发送失败, targetEmail={}, subject={}", scene, targetEmail, subject, exception);
            return "FAILED: " + exception.getMessage();
        }
    }
}
