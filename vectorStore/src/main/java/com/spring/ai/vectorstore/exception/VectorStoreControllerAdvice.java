package com.spring.ai.vectorstore.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 向量库模块全局异常处理器。
 * 负责将模块内部异常转换为统一 HTTP 响应结构。
 */
@RestControllerAdvice(basePackages = "com.spring.ai.vectorstore")
public class VectorStoreControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreControllerAdvice.class);

    /**
     * 处理业务异常。
     *
     * @param exception 业务异常
     * @return 统一响应
     */
    @ExceptionHandler(VectorStoreException.class)
    public ResponseEntity<Map<String, Object>> handleVectorStoreException(VectorStoreException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", exception.getMessage());
        return ResponseEntity.status(exception.getHttpStatus()).body(body);
    }

    /**
     * 处理未捕获异常。
     *
     * @param exception 异常对象
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception exception) {
        log.error("Vector store request failed", exception);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", "Vector store request failed");
        return ResponseEntity.internalServerError().body(body);
    }
}
