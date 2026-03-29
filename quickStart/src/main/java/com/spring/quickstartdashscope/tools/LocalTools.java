package com.spring.quickstartdashscope.tools;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 地点工具类
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/23
 */
public class LocalTools {

    @Tool(description = "If the user needs an address, provide it")
    public String getAddress(@ToolParam(description = "ask for the address") String address, ToolContext toolContext) {
        RunnableConfig runnableConfig = (RunnableConfig) toolContext.getContext().get(AGENT_CONFIG_CONTEXT_KEY);
        Optional<Object> userIdObjOptional = runnableConfig.metadata("user_id");
        String userId = null;
        if (userIdObjOptional.isPresent()) {
            userId = (String) userIdObjOptional.get();
        }
        if (userId == null) {
            userId = "1";
        }

        return "1".equals(userId) ? "anhui hefei" : "anhui wuwei";
    }


}
