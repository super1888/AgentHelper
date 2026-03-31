package com.spring.ai.common.utils;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Authenticator;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.File;
import java.util.Properties;

/**
 * 发送邮件util
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/30
 */
public class EmailUtil {

    // ====================== 你只需要改这里 ======================
    private static final String SMTP_HOST = "smtp.qq.com";      // 邮箱服务器
    private static final String SMTP_PORT = "465";              // 端口
    private static final String FROM_EMAIL = "294419455@qq.com";  // 发件人邮箱
    private static final String AUTH_CODE = "bdsjorboplwccabb";        // 不是密码！是授权码
    // ==========================================================

    /**
     * 发送普通文本邮件
     */
    public static void sendTextMail(String toEmail, String title, String content) throws Exception {
        Session session = createSession();
        Message message = new MimeMessage(session);

        // 发件人、收件人
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(title);
        message.setText(content);

        // 发送
        Transport.send(message);
        System.out.println("纯文本邮件发送成功！");
    }

    /**
     * 发送带附件的邮件（支持HTML）
     */
    public static void sendMailWithAttachment(String toEmail, String title, String htmlContent, String filePath) throws Exception {
        Session session = createSession();
        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(title);

        // 内容 + 附件
        Multipart multipart = new MimeMultipart();

        // 1. 正文（支持HTML）
        BodyPart textPart = new MimeBodyPart();
        textPart.setContent(htmlContent, "text/html;charset=utf-8");
        multipart.addBodyPart(textPart);

        // 2. 附件
        if (filePath != null && new File(filePath).exists()) {
            BodyPart filePart = new MimeBodyPart();
            DataSource source = new FileDataSource(filePath);
            filePart.setDataHandler(new DataHandler(source));
            filePart.setFileName(source.getName());
            multipart.addBodyPart(filePart);
        }

        message.setContent(multipart);
        Transport.send(message);
        System.out.println("带附件邮件发送成功！");
    }

    /**
     * 创建邮件会话
     */
    private static Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.auth", "true");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, AUTH_CODE);
            }
        });
    }

    // ====================== 测试 ======================
    public static void main(String[] args) throws Exception {
        // 发送纯文本
        sendTextMail("3034378787@qq.com", "测试标题", "这是一封Java发送的测试邮件");

        // 发送HTML + 附件
        // sendMailWithAttachment("xxx@qq.com", "带附件测试", "<h2>你好</h2><p>这是HTML内容</p>", "C:/test.txt");
    }

}
