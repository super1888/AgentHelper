package com.spring.quickstart.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 天气工具类
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/23
 */
public class WeatherTools {


    @Tool(description = "Get weather for a given city")
    public String weatherForLocationTool(@ToolParam(description = "The weather") String weather) {
        return "It's always sunny in " + weather + "!";
    }


}
