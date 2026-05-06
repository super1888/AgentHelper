package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.SyUserFaceTemplateMapper;
import com.spring.ai.common.repository.enitiy.SyUserFaceTemplate;
import com.spring.ai.common.repository.service.SyUserFaceTemplateService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

@Service
public class SyUserFaceTemplateServiceImpl extends ServiceImpl<SyUserFaceTemplateMapper, SyUserFaceTemplate>
        implements SyUserFaceTemplateService {

    @Override
    public SyUserFaceTemplate getByUserId(Long userId) {
        return getOne(Wrappers.lambdaQuery(SyUserFaceTemplate.class)
                .eq(SyUserFaceTemplate::getUserId, userId)
                .last(SqlConstants.LIMIT_ONE));
    }
}
