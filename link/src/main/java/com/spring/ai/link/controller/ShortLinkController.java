package com.spring.ai.link.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.link.application.manager.ShortLinkApplicationManager;
import com.spring.ai.link.domain.request.ShortLinkCreateRequest;
import com.spring.ai.link.domain.response.ShortLinkAccessLogResponse;
import com.spring.ai.link.domain.response.ShortLinkResponse;
import com.spring.ai.link.domain.response.ShortLinkStatisticsResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 短链接管理接口。
 */
@RestController
public class ShortLinkController {

    @Resource
    private ShortLinkApplicationManager shortLinkApplicationManager;

    @GetMapping("/short-links")
    public ApiResponse<List<ShortLinkResponse>> listLinks(@RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.success(shortLinkApplicationManager.listLinks(keyword));
    }

    @PostMapping("/short-links")
    public ApiResponse<ShortLinkResponse> createLink(@RequestBody ShortLinkCreateRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(shortLinkApplicationManager.createLink(request, servletRequest));
    }

    @GetMapping("/short-links/{linkId}")
    public ApiResponse<ShortLinkResponse> getLink(@PathVariable Long linkId) {
        return ApiResponse.success(shortLinkApplicationManager.getLink(linkId));
    }

    @PostMapping("/short-links/{linkId}/enable")
    public ApiResponse<ShortLinkResponse> enableLink(@PathVariable Long linkId) {
        return ApiResponse.success(shortLinkApplicationManager.updateStatus(linkId, "ENABLED"));
    }

    @PostMapping("/short-links/{linkId}/disable")
    public ApiResponse<ShortLinkResponse> disableLink(@PathVariable Long linkId) {
        return ApiResponse.success(shortLinkApplicationManager.updateStatus(linkId, "DISABLED"));
    }

    @DeleteMapping("/short-links/{linkId}")
    public ApiResponse<Void> deleteLink(@PathVariable Long linkId) {
        shortLinkApplicationManager.deleteLink(linkId);
        return ApiResponse.success(null);
    }

    @GetMapping("/short-links/statistics")
    public ApiResponse<ShortLinkStatisticsResponse> statistics() {
        return ApiResponse.success(shortLinkApplicationManager.statistics());
    }

    @GetMapping("/short-links/access-logs")
    public ApiResponse<List<ShortLinkAccessLogResponse>> listAccessLogs(@RequestParam(value = "shortCode", required = false) String shortCode) {
        return ApiResponse.success(shortLinkApplicationManager.listAccessLogs(shortCode));
    }
}
