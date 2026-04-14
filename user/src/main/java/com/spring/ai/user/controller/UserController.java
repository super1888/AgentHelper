package com.spring.ai.user.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.user.application.manager.UserAuthApplicationManager;
import com.spring.ai.user.domain.dto.UserLoginRequest;
import com.spring.ai.user.domain.dto.UserRegisterRequest;
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

@RestController
@RequestMapping("/agentHelper/users")
public class UserController {


    @Resource
    UserAuthApplicationManager userAuthApplicationManager;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody UserRegisterRequest request) {
        userAuthApplicationManager.register(request);
        return ApiResponse.success("注册成功", null);
    }

    @PostMapping("/login")
    public ApiResponse<UserAuthLoginVO> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletResponse response) {
        UserAuthLoginVO loginVO = userAuthApplicationManager.login(request);
        response.setHeader(loginVO.getToken().getTokenName(), loginVO.getToken().getAuthorizationValue());
        return ApiResponse.success("登录成功", loginVO);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        userAuthApplicationManager.logout();
        return ApiResponse.success("退出登录成功", null);
    }

    @GetMapping("/getCurrentUser")
    public ApiResponse<UserProfileVO> currentUser() {
        return ApiResponse.success(userAuthApplicationManager.currentUser());
    }
}
