package com.spring.quickstart.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 显式暴露 Agent Studio 页面入口。
 * 某些运行环境下静态资源映射可能未按预期生效，这里提供一个稳定访问路径。
 */
@RestController
public class AgentStudioPageController {

    @GetMapping(value = {"/agent-studio", "/agent-studio.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> page() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new ClassPathResource("static/agent-studio.html"));
    }
}
