package com.spring.ai.opencv.service.impl;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.opencv.domain.request.FaceLoginVerifyRequest;
import com.spring.ai.opencv.domain.response.FaceLoginVerifyResponse;
import com.spring.ai.opencv.service.FaceRecognitionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.stereotype.Service;

/**
 * OpenCV 依赖缺失时的人脸识别降级实现。
 */
@Service
@ConditionalOnMissingClass("org.opencv.core.Mat")
public class UnavailableFaceRecognitionServiceImpl implements FaceRecognitionService {

    private static final String UNAVAILABLE_MESSAGE =
            "Face recognition is unavailable because OpenCV dependency is missing from the runtime classpath";

    /**
     * OpenCV 依赖缺失时，直接返回明确错误。
     */
    @Override
    public FaceLoginVerifyResponse verifyFace(FaceLoginVerifyRequest request) {
        throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, UNAVAILABLE_MESSAGE);
    }

    /**
     * OpenCV 依赖缺失时，直接返回明确错误。
     */
    @Override
    public boolean isSameFace(String sourceEmbedding, String targetEmbedding) {
        throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, UNAVAILABLE_MESSAGE);
    }

    /**
     * OpenCV 依赖缺失时，直接返回明确错误。
     */
    @Override
    public int resolveEmbeddingDimension(String embedding) {
        throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, UNAVAILABLE_MESSAGE);
    }
}
