package com.spring.ai.tools.custom;


import com.spring.ai.common.domain.dto.UserInfoDTO;
import com.spring.ai.common.domain.dto.UserInfoDTO.UserInfoDTOBuilder;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/30
 */
public class GetUserInfoTools {

    @Tool(description = "Get username for a given user information")
    public UserInfoDTO userInfoTool(@ToolParam(description = "The username") String username) {
        UserInfoDTOBuilder builder = UserInfoDTO.builder();
        if ("张三" .equals(username)) {
            builder.name("张三").phone("18226213628").emailAddress("3034378787@qq.com").address("安徽 合肥");
        }

        return builder.build();
    }

}
