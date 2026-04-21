package com.spring.ai.interceptors.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterceptorVersionResponse {

    Long id;

    Integer versionNo;

    String versionCode;

    String versionDescription;

    String versionStatus;

    String publishStatus;

    String snapshotJson;

    Long createTime;
}
