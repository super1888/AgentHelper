package com.spring.ai.core.domain.request;

import java.util.Map;
import lombok.Data;

/**
 * 图片生成代理请求。
 * 负责承接前端页面传入的上游地址、密钥和图片生成载荷，再由服务层转发到目标接口。
 */
@Data
public class ImageGenerationProxyRequest {

    private String baseUrl;

    private String apiKey;

    private String endpointPath;

    private Map<String, Object> payload;
}
