package com.spring.ai.logging.mybatis;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.spring.ai.logging.config.AgentHelperLoggingProperties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 统一 SQL 执行与慢 SQL 日志拦截器。
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, BoundSql.class})
})
public class MybatisSqlLogInterceptor implements Interceptor {

    private static final Logger SQL_LOGGER = LoggerFactory.getLogger("com.spring.ai.logging.sql");

    private static final Logger SLOW_SQL_LOGGER = LoggerFactory.getLogger("com.spring.ai.logging.sql.slow");

    private final AgentHelperLoggingProperties loggingProperties;

    public MybatisSqlLogInterceptor(AgentHelperLoggingProperties loggingProperties) {
        this.loggingProperties = loggingProperties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!loggingProperties.getSql().isEnabled()) {
            return invocation.proceed();
        }

        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameter = args.length > 1 ? args[1] : null;
        BoundSql boundSql = resolveBoundSql(mappedStatement, args, parameter);

        long startNs = System.nanoTime();
        Object result = invocation.proceed();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        String sqlText = buildSql(mappedStatement.getConfiguration(), boundSql);
        String summary = buildSummary(mappedStatement.getId(), elapsedMs, sqlText);
        SQL_LOGGER.info(summary);
        if (elapsedMs >= loggingProperties.getSql().getSlowThresholdMs()) {
            SLOW_SQL_LOGGER.warn(summary);
        }
        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    private BoundSql resolveBoundSql(MappedStatement mappedStatement, Object[] args, Object parameter) {
        if (args.length == 6 && args[5] instanceof BoundSql boundSql) {
            return boundSql;
        }
        return mappedStatement.getBoundSql(parameter);
    }

    private String buildSql(Configuration configuration, BoundSql boundSql) {
        String sql = normalizeSql(boundSql.getSql());
        if (!loggingProperties.getSql().isLogParameters()) {
            return truncate(sql);
        }
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (!CollectionUtils.isEmpty(parameterMappings) && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?", getParameterValue(parameterObject));
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    Object value = null;
                    if (metaObject.hasGetter(propertyName)) {
                        value = metaObject.getValue(propertyName);
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        value = boundSql.getAdditionalParameter(propertyName);
                    }
                    sql = sql.replaceFirst("\\?", getParameterValue(value));
                }
            }
        }
        return truncate(sql);
    }

    private String buildSummary(String sqlId, long elapsedMs, String sqlText) {
        return "sqlId=" + sqlId + ", costMs=" + elapsedMs + ", sql=" + sqlText;
    }

    private String normalizeSql(String sql) {
        return StringUtils.hasText(sql) ? sql.replaceAll("[\\s]+", StringPool.SPACE) : "";
    }

    private String truncate(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        int maxLength = Math.max(200, loggingProperties.getSql().getMaxSqlLength());
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...(truncated)";
    }

    private String getParameterValue(Object value) {
        String text;
        if (value instanceof String stringValue) {
            text = "'" + stringValue + "'";
        } else if (value instanceof Date dateValue) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            text = "'" + formatter.format(dateValue) + "'";
        } else if (value == null) {
            text = "null";
        } else {
            text = value.toString();
        }
        return text.replace("$", "\\$");
    }
}
