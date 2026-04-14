package com.spring.ai.common.utils;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.ConstVal;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.TemplateConfig;
import com.baomidou.mybatisplus.generator.config.po.TableFill;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * common 模块代码生成工具。
 * 生成结果固定输出到 common/repository 目录结构下：
 * enitiy、dao、service、service/impl
 */
public final class CommonRepositoryCodeGeneratorUtil {

    // 定义基础包路径，用于生成的代码存放
    private static final String BASE_PACKAGE = "com.spring.ai.common.repository";
    // 实体类包名
    private static final String ENTITY_PACKAGE = "enitiy";
    // 数据访问对象(DAO)包名
    private static final String MAPPER_PACKAGE = "dao";
    // 服务接口包名
    private static final String SERVICE_PACKAGE = "service";
    // 服务实现类包名
    private static final String SERVICE_IMPL_PACKAGE = "service.impl";
    // 基础实体类全限定名，所有生成的实体类都将继承此类
    private static final String SUPER_ENTITY_CLASS = "com.spring.ai.common.domain.base.BaseEntity";

    /**
     * 数据库连接配置信息
     * 直接改这里即可。
     */
    // 数据库连接URL
    private static final String URL = "jdbc:mysql://localhost:3306/agent_db?rewriteBatchedStatements=true&autoReconnect=true&allowMultiQueries=true&useSSL=false&characterEncoding=UTF-8&serverTimezone=GMT%2B8";
    // 数据库用户名
    private static final String USERNAME = "root";
    // 数据库密码
    private static final String PASSWORD = "root";
    // JDBC驱动类名
    private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
    // 作者名称
    private static final String AUTHOR = "auto-generator";
    // 是否覆盖已存在文件
    private static final boolean FILE_OVERRIDE = false;
    // 需要生成代码的表名列表
    private static final String[] TABLES = {
            "sy_user"
    };
    // 表名前缀列表，用于过滤表名
    private static final String[] TABLE_PREFIXES = {
    };

    // 私有构造方法，防止实例化工具类
    private CommonRepositoryCodeGeneratorUtil() {
    }

    // 程序入口方法
    public static void main(String[] args) {
        // 创建并配置代码生成器
        GeneratorOptions options = GeneratorOptions.defaultOptions();
        AutoGenerator generator = new AutoGenerator();
        generator.setGlobalConfig(buildGlobalConfig(options));
        generator.setDataSource(buildDataSourceConfig(options));
        generator.setPackageInfo(buildPackageConfig(options));
        generator.setCfg(buildInjectionConfig());
        generator.setTemplate(buildTemplateConfig());
        generator.setStrategy(buildStrategyConfig(options));
        generator.execute();
    }

    /**
     * 构建全局配置
     * @param options 生成器选项
     * @return 全局配置对象
     */
    private static GlobalConfig buildGlobalConfig(GeneratorOptions options) {
        GlobalConfig config = new GlobalConfig();
        config.setOutputDir(options.outputDir);
        config.setAuthor(options.author);
        config.setOpen(false);
        config.setFileOverride(options.fileOverride);
        config.setBaseResultMap(true);
        config.setBaseColumnList(true);
        config.setServiceName("%sService");
        config.setDateType(DateType.TIME_PACK);
        return config;
    }

    /**
     * 构建数据源配置
     * @param options 生成器选项
     * @return 数据源配置对象
     */
    private static DataSourceConfig buildDataSourceConfig(GeneratorOptions options) {
        DataSourceConfig config = new DataSourceConfig();
        config.setUrl(options.url);
        config.setDriverName(options.driverClassName);
        config.setUsername(options.username);
        config.setPassword(options.password);
        return config;
    }

    /**
     * 构建包信息配置
     * @param options 生成器选项
     * @return 包配置对象
     */
    private static PackageConfig buildPackageConfig(GeneratorOptions options) {
        PackageConfig config = new PackageConfig();
        config.setParent(BASE_PACKAGE);
        config.setEntity(ENTITY_PACKAGE);
        config.setMapper(MAPPER_PACKAGE);
        config.setService(SERVICE_PACKAGE);
        config.setServiceImpl(SERVICE_IMPL_PACKAGE);
        config.setPathInfo(buildPathInfo(options.outputDir));
        return config;
    }

    /**
     * 构建路径信息
     * @param outputDir 输出目录
     * @return 路径信息映射
     */
    private static Map<String, String> buildPathInfo(String outputDir) {
        Map<String, String> packageInfo = new HashMap<>(4);
        packageInfo.put(ConstVal.ENTITY, BASE_PACKAGE + "." + ENTITY_PACKAGE);
        packageInfo.put(ConstVal.MAPPER, BASE_PACKAGE + "." + MAPPER_PACKAGE);
        packageInfo.put(ConstVal.SERVICE, BASE_PACKAGE + "." + SERVICE_PACKAGE);
        packageInfo.put(ConstVal.SERVICE_IMPL, BASE_PACKAGE + "." + SERVICE_IMPL_PACKAGE);

        Map<String, String> pathInfo = new HashMap<>(4);
        pathInfo.put(ConstVal.ENTITY_PATH, buildJavaPath(outputDir, packageInfo.get(ConstVal.ENTITY)));
        pathInfo.put(ConstVal.MAPPER_PATH, buildJavaPath(outputDir, packageInfo.get(ConstVal.MAPPER)));
        pathInfo.put(ConstVal.SERVICE_PATH, buildJavaPath(outputDir, packageInfo.get(ConstVal.SERVICE)));
        pathInfo.put(ConstVal.SERVICE_IMPL_PATH, buildJavaPath(outputDir, packageInfo.get(ConstVal.SERVICE_IMPL)));
        return pathInfo;
    }

    /**
     * 构建Java文件路径
     * @param outputDir 输出目录
     * @param packageName 包名
     * @return Java文件路径
     */
    private static String buildJavaPath(String outputDir, String packageName) {
        return outputDir + File.separator + packageName.replace(StringPool.DOT, File.separator);
    }

    /**
     * 构建注入配置
     * @return 注入配置对象
     */
    private static InjectionConfig buildInjectionConfig() {
        return new InjectionConfig() {
            @Override
            public void initMap() {
                // no-op
            }
        };
    }

    /**
     * 构建模板配置
     * @return 模板配置对象
     */
    private static TemplateConfig buildTemplateConfig() {
        TemplateConfig config = new TemplateConfig();
        config.setController(null);
        config.setXml(null);
        return config;
    }

    /**
     * 构建策略配置
     * @param options 生成器选项
     * @return 策略配置对象
     */
    private static StrategyConfig buildStrategyConfig(GeneratorOptions options) {
        StrategyConfig config = new StrategyConfig();
        config.setNaming(NamingStrategy.underline_to_camel);
        config.setColumnNaming(NamingStrategy.underline_to_camel);
        config.setEntityLombokModel(true);
        config.setInclude(options.tables);
        config.setTablePrefix(options.tablePrefixes);
        config.setSuperEntityClass(SUPER_ENTITY_CLASS);
        config.setSuperEntityColumns(
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
        );
        config.setEntityTableFieldAnnotationEnable(true);
        config.setEntitySerialVersionUID(false);
        config.setVersionFieldName("version");
        config.setTableFillList(buildTableFills());
        return config;
    }

    /**
     * 构建表填充字段配置
     * @return 表填充字段列表
     */
    private static List<TableFill> buildTableFills() {
        List<TableFill> tableFills = new ArrayList<>(2);
        tableFills.add(new TableFill("create_time", FieldFill.INSERT));
        tableFills.add(new TableFill("update_time", FieldFill.INSERT_UPDATE));
        return tableFills;
    }

    /**
     * 生成器选项内部类
     * 封装代码生成所需的各项配置选项
     */
    private static final class GeneratorOptions {

        // 数据库连接URL
        private final String url;
        // 数据库用户名
        private final String username;
        // 数据库密码
        private final String password;
        // JDBC驱动类名
        private final String driverClassName;
        // 作者名称
        private final String author;
        // 输出目录
        private final String outputDir;
        // 需要生成代码的表名列表
        private final String[] tables;
        // 表名前缀列表
        private final String[] tablePrefixes;
        // 是否覆盖已存在文件
        private final boolean fileOverride;

        /**
         * 构造方法
         */
        private GeneratorOptions(
                String url,
                String username,
                String password,
                String driverClassName,
                String author,
                String outputDir,
                String[] tables,
                String[] tablePrefixes,
                boolean fileOverride) {
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

        /**
         * 创建默认选项
         * @return 默认选项对象
         */
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

        /**
         * 解析默认输出目录
         * @return 默认输出目录路径
         */
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
