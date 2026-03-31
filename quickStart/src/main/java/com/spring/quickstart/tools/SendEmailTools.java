package com.spring.quickstart.tools;


import com.spring.ai.common.utils.EmailUtil;
import com.spring.quickstart.model.dto.UserInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/30
 */
@Slf4j
public class SendEmailTools {

    /**
     * 根据用户名获取信息，并直接发送邮件（AI 只需要调用这一个）
     */
    @Tool(description = "根据用户名获取用户信息，并自动给该用户发送邮件")
    public String getUserInfoAndSendEmail(
            @ToolParam(description = "用户名，例如：张三") String username,
            @ToolParam(description = "邮件标题") String title,
            @ToolParam(description = "邮件内容") String content
    ) {
        try {
            // 1. 先查用户信息
            UserInfoDTO user;
            if ("张三".equals(username)) {
                user = UserInfoDTO.builder()
                        .name("张三")
                        .phone("18226213628")
                        .emailAddress("3034378787@qq.com")
                        .address("安徽 合肥")
                        .build();
            } else {
                return "用户不存在：" + username;
            }

            // 2. 直接发送（真实邮箱！）
            String email = user.getEmailAddress();
            log.info("【真实发送】to: {}, title: {}, content: {}", email, title, content);
            EmailUtil.sendTextMail(email, title, content);

            return "发送成功！用户信息：" + user.toString();
        } catch (Exception e) {
            log.error("发送失败", e);
            return "发送失败：" + e.getMessage();
        }
    }

}
