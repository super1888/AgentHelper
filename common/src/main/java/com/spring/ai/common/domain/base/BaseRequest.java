package com.spring.ai.common.domain.base;

import lombok.Data;


/**
 * @Description: 基础VO
 * @Author: zhouqi
 */
@Data
public class BaseRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 20;


}
