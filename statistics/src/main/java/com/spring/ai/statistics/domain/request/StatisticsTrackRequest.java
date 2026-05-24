package com.spring.ai.statistics.domain.request;

import lombok.Data;

@Data
public class StatisticsTrackRequest {

    private String path;
    private String title;
    private String visitorId;
    private String visitId;
}