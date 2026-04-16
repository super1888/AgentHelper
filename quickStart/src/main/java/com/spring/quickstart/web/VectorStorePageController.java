package com.spring.quickstart.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件用途：向量存储管理页路由控制器
 * 作者：Codex
 * 创建时间：2026-04-16
 * 核心功能：为向量存储管理页面提供稳定访问入口，避免静态资源映射受部署环境影响。
 */
@RestController
public class VectorStorePageController {

    /**
     * 返回向量存储管理页面。
     *
     * @return HTML 页面资源
     */
    @GetMapping(value = {"/vector-store-manager", "/vector-store-manager.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> page() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new ClassPathResource("static/vector-store-manager.html"));
    }
}
