package com.spring.ai.opencv.service;

import com.spring.ai.opencv.domain.request.FaceLoginVerifyRequest;
import com.spring.ai.opencv.domain.response.FaceLoginVerifyResponse;

/**
 * 人脸识别服务。
 */
public interface FaceRecognitionService {

    FaceLoginVerifyResponse verifyFace(FaceLoginVerifyRequest request);

    boolean isSameFace(String sourceFingerprint, String targetFingerprint);
}
