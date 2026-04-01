package com.spring.ai.common.bean.customWord;

import com.github.houbb.sensitive.word.api.IWordDeny;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/31
 */
@Component
public class MyDdWordDeny implements IWordDeny {

    @Override
    public List<String> deny() {
        return List.of();
    }
}
