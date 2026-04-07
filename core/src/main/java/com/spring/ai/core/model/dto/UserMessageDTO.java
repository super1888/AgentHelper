package com.spring.ai.core.model.dto;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/29
 */

import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.UserMessage.Builder;
import org.springframework.ai.content.Media;
import org.springframework.core.io.Resource;

@Data
public class UserMessageDTO {


    /**
     * 用户输入文本内容
     */
    private String textContent;


    /**
     * 用户输入的文件资源
     */
    private Resource resource;

    /**
     * 用户输入的媒体内容
     */
    private List<Media> media;

    /**
     * 存入上下文用户信息
     */
    private Map<String, Object> metadata;

    public static UserMessage getUserMessage(String textContent, Resource resource, List<Media> media, Map<String, Object> metadata) {
        Builder builder = UserMessage.builder();

        if (textContent != null) {
            builder.text(textContent);
        }

        if (resource != null) {
            builder.text(resource);
        }

        if (media != null) {
            builder.media(media);
        }

        if (metadata != null) {
            builder.metadata(metadata);
        }

        return builder.build();
    }

}




