package com.spring.ai.user.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.SaTokenException;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.spring.ai.user")
public class UserAuthExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLoginException(NotLoginException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ErrorCodeEnum.UNAUTHORIZED, exception.getMessage()));
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotPermissionException(NotPermissionException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ErrorCodeEnum.FORBIDDEN, exception.getMessage()));
    }

    @ExceptionHandler(SaTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleSaTokenException(SaTokenException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ErrorCodeEnum.UNAUTHORIZED, exception.getMessage()));
    }
}
