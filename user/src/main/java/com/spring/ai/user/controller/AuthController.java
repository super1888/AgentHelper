package com.spring.ai.user.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.user.application.manager.AuthApplicationManager;
import com.spring.ai.user.domain.request.UserLoginRequest;
import com.spring.ai.user.domain.vo.UserAuthLoginVO;
import com.spring.ai.user.domain.vo.UserProfileVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/14
 */
@RestController
@RequestMapping("/agentHelper/auth")
public class AuthController {

    @Resource
    AuthApplicationManager authApplicationManager;

    @PostMapping("/login")
    public ApiResponse<UserAuthLoginVO> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletResponse response) {
        UserAuthLoginVO loginVO = authApplicationManager.login(request);
        response.setHeader(loginVO.getToken().getTokenName(), loginVO.getToken().getAuthorizationValue());
        return ApiResponse.success("登录成功", loginVO);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authApplicationManager.logout();
        return ApiResponse.success("退出登录成功", null);
    }

    @GetMapping("/getCurrentUser")
    public ApiResponse<UserProfileVO> currentUser() {
        return ApiResponse.success(authApplicationManager.currentUser());
    }
}
