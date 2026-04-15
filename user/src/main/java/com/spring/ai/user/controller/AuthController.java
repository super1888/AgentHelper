package com.spring.ai.user.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.user.application.manager.AuthApplicationManager;
import com.spring.ai.user.domain.request.UserLoginRequest;
import com.spring.ai.user.domain.vo.UserAuthLoginVO;
import com.spring.ai.user.domain.vo.UserProfileVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationManager authApplicationManager;

    /**
     * 用户登录。
     */
    @PostMapping("/login")
    public ApiResponse<UserAuthLoginVO> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletResponse response
    ) {
        UserAuthLoginVO loginVO = authApplicationManager.login(request);
        response.setHeader(loginVO.getToken().getTokenName(), loginVO.getToken().getAuthorizationValue());
        return ApiResponse.success("登录成功", loginVO);
    }

    /**
     * 退出登录。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authApplicationManager.logout();
        return ApiResponse.success("退出登录成功", null);
    }

    /**
     * 查询当前登录用户。
     */
    @GetMapping("/currentUser")
    public ApiResponse<UserProfileVO> currentUser() {
        return ApiResponse.success(authApplicationManager.currentUser());
    }
}
