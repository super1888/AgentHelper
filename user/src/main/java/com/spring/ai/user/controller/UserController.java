package com.spring.ai.user.controller;

import com.github.pagehelper.PageInfo;
import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.user.application.manager.UserApplicationManager;
import com.spring.ai.user.domain.request.UserCreateRequest;
import com.spring.ai.user.domain.request.UserPageQueryRequest;
import com.spring.ai.user.domain.request.UserRegisterRequest;
import com.spring.ai.user.domain.request.UserUpdateRequest;
import com.spring.ai.user.domain.vo.UserProfileVO;
import com.spring.ai.user.domain.vo.UserStatisticsVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制器。
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationManager userApplicationManager;

    /**
     * 用户注册。
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody UserRegisterRequest request) {
        userApplicationManager.register(request);
        return ApiResponse.success("注册成功", null);
    }

    /**
     * 新增用户。
     */
    @PostMapping("/add")
    public ApiResponse<Void> createUser(@Valid @RequestBody UserCreateRequest request) {
        userApplicationManager.createUser(request);
        return ApiResponse.success("新增用户成功", null);
    }

    /**
     * 删除用户。
     */
    @DeleteMapping("/delete/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        userApplicationManager.deleteUser(userId);
        return ApiResponse.success("删除用户成功", null);
    }

    /**
     * 更新用户。
     */
    @PutMapping("/update/{userId}")
    public ApiResponse<Void> updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateRequest request) {
        userApplicationManager.updateUser(userId, request);
        return ApiResponse.success("编辑用户成功", null);
    }

    /**
     * 查询用户详情。
     */
    @GetMapping("/select/{userId}")
    public ApiResponse<UserProfileVO> getUserDetail(@PathVariable Long userId) {
        return ApiResponse.success(userApplicationManager.getUserDetail(userId));
    }

    /**
     * 查询全部用户。
     */
    @GetMapping("/getAllUser")
    public ApiResponse<List<UserProfileVO>> listAllUsers() {
        return ApiResponse.success(userApplicationManager.listAllUsers());
    }

    /**
     * 分页条件查询。
     */
    @PostMapping("/pageQuery")
    public ApiResponse<PageInfo<UserProfileVO>> pageQuery(@RequestBody(required = false) UserPageQueryRequest request) {
        return ApiResponse.success(userApplicationManager.pageQueryUsers(
                request == null ? new UserPageQueryRequest() : request));
    }

    /**
     * 用户统计。
     */
    @PostMapping("/statistics")
    public ApiResponse<UserStatisticsVO> statistics() {
        return ApiResponse.success(userApplicationManager.userStatistics());
    }
}
