package com.spring.ai.bigfile.domain.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BigFileListResponse {

    private List<BigFileRecordResponse> items;
}
