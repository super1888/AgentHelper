package com.spring.ai.tools.custom;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 工具类
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/23
 */
public class CalculatorTools {

    @Tool(description = "Add two numbers together")
    public String add(
            @ToolParam(description = "First number") int a,
            @ToolParam(description = "Second number") int b) {
        return String.valueOf(a + b);
    }

    @Tool(description = "Multiply two numbers together")
    public String multiply(
            @ToolParam(description = "First number") int a,
            @ToolParam(description = "Second number") int b) {
        return String.valueOf(a * b);
    }

    @Tool(description = "give a random number")
    public String randomNumberTool() {
        return "random number" + "11223344" + "!";
    }


}
