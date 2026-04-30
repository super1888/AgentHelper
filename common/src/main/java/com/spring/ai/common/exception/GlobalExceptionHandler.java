package com.spring.ai.common.exception;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p>统一收敛业务异常、参数异常与系统异常，输出结构化异常日志，便于线上排查。</p>
 */
@RestControllerAdvice(basePackages = "com.spring.ai")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger("com.spring.ai.logging.exception");

    /**
     * 处理业务异常。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception,
                                                                     HttpServletRequest request) {
        log.warn("traceId={}, type=BUSINESS, method={}, uri={}, status={}, code={}, message={}",
                currentTraceId(), request.getMethod(), request.getRequestURI(),
                exception.getHttpStatus().value(), exception.getCode(), exception.getMessage());
        return ResponseEntity.status(exception.getHttpStatus())
                .body(ApiResponse.fail(exception.getCode(), exception.getMessage()));
    }

    /**
     * 处理参数类异常。
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentNotValidException.class,
            BindException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(Exception exception,
                                                                       HttpServletRequest request) {
        log.warn("traceId={}, type=BAD_REQUEST, method={}, uri={}, status=400, error={}",
                currentTraceId(), request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCodeEnum.BAD_REQUEST, exception.getMessage()));
    }

    /**
     * 处理系统异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandledException(Exception exception,
                                                                      HttpServletRequest request) {
        log.error("traceId={}, type=SYSTEM, method={}, uri={}, status=500, error={}",
                currentTraceId(), request.getMethod(), request.getRequestURI(), exception.getMessage(), exception);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail(
                        ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                        ErrorCodeEnum.INTERNAL_SERVER_ERROR.getMessage()
                ));
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId == null ? "NO_TRACE" : traceId;
    }
}
