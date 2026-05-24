package com.spring.ai.statistics.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatisticsTrackResponse {

    private String date;
    private String path;
    private String visitorId;
    private String visitId;
    private String message;
}