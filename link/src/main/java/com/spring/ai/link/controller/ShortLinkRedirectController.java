package com.spring.ai.link.controller;

import com.spring.ai.link.application.manager.ShortLinkApplicationManager;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;

/**
 * 短链接公开跳转入口。
 */
@Controller
public class ShortLinkRedirectController {

    @Resource
    private ShortLinkApplicationManager shortLinkApplicationManager;

    /**
     * 处理根路径短链接跳转，避免被统一 API 前缀影响。
     */
    @GetMapping("/s/{shortCode}")
    public void redirect(@PathVariable String shortCode, HttpServletRequest request, HttpServletResponse response) throws IOException {
        shortLinkApplicationManager.redirect(shortCode, request, response);
    }
}
