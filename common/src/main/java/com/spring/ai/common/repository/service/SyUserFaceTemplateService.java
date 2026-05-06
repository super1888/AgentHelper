package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.SyUserFaceTemplate;

public interface SyUserFaceTemplateService extends IService<SyUserFaceTemplate> {

    SyUserFaceTemplate getByUserId(Long userId);
}
