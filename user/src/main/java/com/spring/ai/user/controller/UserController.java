package com.spring.ai.user.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.user.application.manager.UserApplicationManager;
import com.spring.ai.user.domain.request.UserRegisterRequest;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agentHelper/users")
public class UserController {


    @Resource
    UserApplicationManager userApplicationManager;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody UserRegisterRequest request) {
        userApplicationManager.register(request);
        return ApiResponse.success("注册成功", null);
    }

}
