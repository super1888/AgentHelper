package com.spring.ai.user.application.service;

import com.spring.ai.opencv.domain.request.FaceLoginVerifyRequest;
import com.spring.ai.opencv.domain.response.FaceLoginVerifyResponse;

/**
 * 用户模块人脸识别服务抽象。
 */
public interface UserFaceRecognitionService {

    /**
     * 校验图片并提取人脸特征。
     */
    FaceLoginVerifyResponse verifyFace(FaceLoginVerifyRequest request);

    /**
     * 比较两组特征是否属于同一人。
     */
    boolean isSameFace(String sourceEmbedding, String targetEmbedding);

    /**
     * 解析特征维度。
     */
    int resolveEmbeddingDimension(String embedding);

    /**
     * 返回当前特征版本。
     */
    String resolveEmbeddingVersion();
}
