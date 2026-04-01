package com.spring.ai.common.bean.customWord;

import com.github.houbb.sensitive.word.api.IWordAllow;
import java.util.List;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/31
 */

public class MyDdWordAllow implements IWordAllow {

    @Override
    public List<String> allow() {
        return List.of();
    }
}
