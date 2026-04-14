package com.spring.quickstart.config.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * 公共字段自动填充处理器。
 */
@Component
public class CommonMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "version", Integer.class, 0);
        // ===================== 新增：创建人信息 =====================
//        this.strictInsertFill(metaObject, "createId", Long.class, getCurrentUserId());
//        this.strictInsertFill(metaObject, "createName", String.class, getCurrentUserName());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        // ===================== 新增：更新人信息 =====================
//        this.strictUpdateFill(metaObject, "updateId", Long.class, getCurrentUserId());
//        this.strictUpdateFill(metaObject, "updateName", String.class, getCurrentUserName());
    }
}
