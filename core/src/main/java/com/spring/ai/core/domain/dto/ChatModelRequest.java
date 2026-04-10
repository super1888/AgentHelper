package com.spring.ai.core.domain.dto;

import lombok.Data;

@Data
public class ChatModelRequest {

    private String provider;

    private String model;

    private String apiKey;

    private ChatOptionsDTO options;
}
