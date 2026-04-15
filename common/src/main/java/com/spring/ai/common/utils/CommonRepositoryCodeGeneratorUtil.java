package com.spring.ai.common.utils;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.IFill;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * common 模块仓储代码生成工具。
 *
 * <p>适配 MyBatis-Plus 3.5.15 新版生成器，生成结果固定输出到
 * common/repository 目录结构下：enitiy、dao、service、service/impl。</p>
 */
public final class CommonRepositoryCodeGeneratorUtil {

    /**
     * 这里保持为仓储层父包，子包继续按现有目录输出。
     */
    private static final String BASE_PACKAGE = "com.spring.ai.common.repository";
    private static final String ENTITY_PACKAGE = "enitiy";
    private static final String MAPPER_PACKAGE = "dao";
    private static final String SERVICE_PACKAGE = "service";
    private static final String SERVICE_IMPL_PACKAGE = "service.impl";
    private static final String SUPER_ENTITY_CLASS = "com.spring.ai.common.domain.base.BaseEntity";

    /**
     * 数据库连接配置，按需直接修改这里即可。
     */
    private static final String URL =
            "jdbc:mysql://localhost:3306/agent_db?rewriteBatchedStatements=true&autoReconnect=true"
                    + "&allowMultiQueries=true&useSSL=false&characterEncoding=UTF-8&serverTimezone=GMT%2B8";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";
    private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
    private static final String AUTHOR = "auto-generator";
    private static final boolean FILE_OVERRIDE = false;
    private static final String[] TABLES = {
            "sy_user"
    };
    private static final String[] TABLE_PREFIXES = {};

    private CommonRepositoryCodeGeneratorUtil() {
    }

    public static void main(String[] args) {
        GeneratorOptions options = GeneratorOptions.defaultOptions();

        FastAutoGenerator.create(options.url, options.username, options.password)
                .globalConfig(builder -> builder
                        .author(options.author)
                        .disableOpenDir()
                        .outputDir(options.outputDir)
                        .dateType(DateType.TIME_PACK)
                )
                .packageConfig(builder -> builder
                        .parent(BASE_PACKAGE)
                        .entity(ENTITY_PACKAGE)
                        .mapper(MAPPER_PACKAGE)
                        .service(SERVICE_PACKAGE)
                        .serviceImpl(SERVICE_IMPL_PACKAGE)
                )
                .strategyConfig(builder -> {
                    builder.addInclude(options.tables);
                    if (options.tablePrefixes.length > 0) {
                        builder.addTablePrefix(options.tablePrefixes);
                    }

                    var entityBuilder = builder.entityBuilder()
                            .superClass(SUPER_ENTITY_CLASS)
                            .naming(NamingStrategy.underline_to_camel)
                            .columnNaming(NamingStrategy.underline_to_camel)
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .disableSerialVersionUID()
                            .versionColumnName("version")
                            .addSuperEntityColumns(
                                    "id",
                                    "ext",
                                    "remark",
                                    "create_id",
                                    "create_name",
                                    "create_time",
                                    "update_id",
                                    "update_name",
                                    "update_time",
                                    "version"
                            )
                            .addTableFills(buildTableFills());
                    if (options.fileOverride) {
                        entityBuilder.enableFileOverride();
                    }

                    var mapperBuilder = builder.mapperBuilder()
                            .enableBaseResultMap()
                            .enableBaseColumnList();
                    if (options.fileOverride) {
                        mapperBuilder.enableFileOverride();
                    }

                    var serviceBuilder = builder.serviceBuilder()
                            .formatServiceFileName("%sService");
                    if (options.fileOverride) {
                        serviceBuilder.enableFileOverride();
                    }
                })
                .templateConfig(builder -> builder.disable(TemplateType.XML, TemplateType.CONTROLLER))
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }

    /**
     * 自动填充字段统一在这里维护。
     */
    private static List<IFill> buildTableFills() {
        List<IFill> tableFills = new ArrayList<>(2);
        tableFills.add(new Column("create_time", FieldFill.INSERT));
        tableFills.add(new Column("update_time", FieldFill.INSERT_UPDATE));
        return tableFills;
    }

    /**
     * 生成器运行参数。
     */
    private static final class GeneratorOptions {

        private final String url;
        private final String username;
        private final String password;
        private final String driverClassName;
        private final String author;
        private final String outputDir;
        private final String[] tables;
        private final String[] tablePrefixes;
        private final boolean fileOverride;

        private GeneratorOptions(
                String url,
                String username,
                String password,
                String driverClassName,
                String author,
                String outputDir,
                String[] tables,
                String[] tablePrefixes,
                boolean fileOverride
        ) {
            this.url = url;
            this.username = username;
            this.password = password;
            this.driverClassName = driverClassName;
            this.author = author;
            this.outputDir = outputDir;
            this.tables = tables;
            this.tablePrefixes = tablePrefixes;
            this.fileOverride = fileOverride;
        }

        private static GeneratorOptions defaultOptions() {
            return new GeneratorOptions(
                    URL,
                    USERNAME,
                    PASSWORD,
                    DRIVER_CLASS_NAME,
                    AUTHOR,
                    resolveDefaultOutputDir(),
                    TABLES,
                    TABLE_PREFIXES,
                    FILE_OVERRIDE
            );
        }

        private static String resolveDefaultOutputDir() {
            String userDir = System.getProperty("user.dir");
            File currentDir = new File(userDir);
            if ("common".equals(currentDir.getName())) {
                return userDir + File.separator + "src" + File.separator + "main" + File.separator + "java";
            }
            return userDir + File.separator + "common"
                    + File.separator + "src" + File.separator + "main"
                    + File.separator + "java";
        }
    }
}
