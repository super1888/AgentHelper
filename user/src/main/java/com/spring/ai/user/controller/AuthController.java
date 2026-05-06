package com.spring.ai.user.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.user.application.manager.AuthApplicationManager;
import com.spring.ai.user.application.manager.UserFaceAuthManager;
import com.spring.ai.user.domain.request.UserFaceBindRequest;
import com.spring.ai.user.domain.request.UserFaceLoginRequest;
import com.spring.ai.user.domain.request.UserLoginRequest;
import com.spring.ai.user.domain.vo.UserAuthLoginVO;
import com.spring.ai.user.domain.vo.UserFaceBindVO;
import com.spring.ai.user.domain.vo.UserFaceStatusVO;
import com.spring.ai.user.domain.vo.UserProfileVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth controller.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationManager authApplicationManager;
    private final UserFaceAuthManager userFaceAuthManager;

    @PostMapping("/login")
    public ApiResponse<UserAuthLoginVO> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletResponse response
    ) {
        UserAuthLoginVO loginVO = authApplicationManager.login(request);
        response.setHeader(loginVO.getToken().getTokenName(), loginVO.getToken().getAuthorizationValue());
        return ApiResponse.success("Login success", loginVO);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authApplicationManager.logout();
        return ApiResponse.success("Logout success", null);
    }

    @GetMapping("/currentUser")
    public ApiResponse<UserProfileVO> currentUser() {
        return ApiResponse.success(authApplicationManager.currentUser());
    }

    @PostMapping("/face/bind")
    public ApiResponse<UserFaceBindVO> bindFace(@Valid @RequestBody UserFaceBindRequest request) {
        return ApiResponse.success("Face bind success", userFaceAuthManager.bindFace(request));
    }

    @PostMapping("/face/login")
    public ApiResponse<UserAuthLoginVO> faceLogin(
            @Valid @RequestBody UserFaceLoginRequest request,
            HttpServletResponse response
    ) {
        UserAuthLoginVO loginVO = userFaceAuthManager.faceLogin(request);
        response.setHeader(loginVO.getToken().getTokenName(), loginVO.getToken().getAuthorizationValue());
        return ApiResponse.success("Face login success", loginVO);
    }

    @GetMapping("/face/status")
    public ApiResponse<UserFaceStatusVO> faceStatus() {
        return ApiResponse.success("Query success", userFaceAuthManager.faceStatus());
    }

    @DeleteMapping("/face/unbind")
    public ApiResponse<Void> unbindFace() {
        userFaceAuthManager.unbindFace();
        return ApiResponse.success("Unbind success", null);
    }
}
