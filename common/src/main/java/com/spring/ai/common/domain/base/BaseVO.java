package com.spring.ai.common.domain.base;

import java.util.Date;
import lombok.Data;


/**
 * @Description: 基础VO
 * @Author: zhouqi
 */
@Data
public class BaseVO {

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    private Long id;

    private Long deleted;

    private String ext;

    private String remark;

    private Date createTime;

    private String creator;

    private Date modifyTime;

    private String modifier;

}
