package com.spring.ai.common.utils;

//import com.baomidou.mybatisplus.annotation.DbType;
//import com.baomidou.mybatisplus.core.toolkit.StringPool;
//import com.baomidou.mybatisplus.generator.AutoGenerator;
//import com.baomidou.mybatisplus.generator.InjectionConfig;
//import com.baomidou.mybatisplus.generator.config.ConstVal;
//import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
//import com.baomidou.mybatisplus.generator.config.GlobalConfig;
//import com.baomidou.mybatisplus.generator.config.PackageConfig;
//import com.baomidou.mybatisplus.generator.config.StrategyConfig;
//import com.baomidou.mybatisplus.generator.config.TemplateConfig;
//import com.baomidou.mybatisplus.generator.config.rules.DateType;
//import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
//import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

public class CodeOracleGeneratorUtil {

//    public static void main(String[] args) {
//
//        String[] tables = new String[]{
//                "MT_INFO_STATUS",
//                "MT_P_CODE_SORT",
//                "MT_P_CODE",
//                "MT_METER",
//                "MT_IT",
//                "MT_LC_EQUIP",
//                "MT_ERR_INFO_DEFINE",
//                "MT_ASSIST_EQUIP_INF",
//                "P_USER"};
//
//        //代码生成器
//        AutoGenerator mpg = new AutoGenerator();
//
//        //全局配置
//        GlobalConfig gc = new GlobalConfig();
//        String projectPath = System.getProperty("user.dir");
//        String srcPath = projectPath + "/xckj-magic-cube-biz/src/main/java";
//        gc.setOutputDir(srcPath);
//        gc.setAuthor("自动生成");
//        gc.setFileOverride(true);
//        gc.setServiceName("%sService");//自定义Service接口生成的文件名
//        gc.setOpen(false);
//        gc.setBaseResultMap(true);
//        gc.setDateType(DateType.ONLY_DATE);
//        gc.setSwagger2(true);
//        mpg.setGlobalConfig(gc);
//
//        //数据源配置
//        DataSourceConfig dsc = new DataSourceConfig();
//        dsc.setUrl("jdbc:oracle:thin:@//10.250.251.106:1521/oracletrue")
//                .setDriverName("oracle.jdbc.driver.OracleDriver")
//                .setUsername("XCMAGICCUBE")
//                .setPassword("Magic@2022xckj")
//                .setDbType(DbType.ORACLE);
//        mpg.setDataSource(dsc);
//
//        //包配置
//        PackageConfig pc = new PackageConfig();
//        pc.setEntity("com.xckj.magic.cube.mds.repository.bean.a_mt")
//                .setParent("")
//                .setMapper("com.xckj.magic.cube.mds.repository.dao.a_mt")
//                .setService("com.xckj.magic.cube.mds.service.a_mt")
//                .setServiceImpl("com.xckj.magic.cube.mds.service.a_mt.impl")
//        ;
//
//        Map<String, String> packageInfo = new HashMap<>();
//        packageInfo.put(ConstVal.ENTITY, "com.xckj.magic.cube.mds.repository.bean.a_mt");
//        packageInfo.put(ConstVal.MAPPER, "com.xckj.magic.cube.mds.repository.dao.a_mt");
//
//        packageInfo.put(ConstVal.SERVICE, "com.xckj.magic.cube.mds.service.a_mt");
//        packageInfo.put(ConstVal.SERVICE_IMPL, "com.xckj.magic.cube.mds.service.a_mt.impl");
//        /*
//         * pathInfo配置controller、service、serviceImpl、entity、mapper、mapper.xml等文件的生成路径
//         * srcPath也可以更具实际情况灵活配置
//         * 后面部分的路径是和上面packageInfo包路径对应的源码文件夹路径
//         * 这里你可以选择注释其中某些路径，可忽略生成该类型的文件，例如:注释掉下面pathInfo中Controller的路径，就不会生成Controller文件
//         */
//        Map<String, String> pathInfo = new HashMap<>(8);
//        pathInfo.put(ConstVal.ENTITY_PATH, srcPath + "/" + packageInfo.get(ConstVal.ENTITY).replaceAll("\\.", StringPool.BACK_SLASH + File.separator));
//        pathInfo.put(ConstVal.MAPPER_PATH, srcPath + "/" + packageInfo.get(ConstVal.MAPPER).replaceAll("\\.", StringPool.BACK_SLASH + File.separator));
//        pathInfo.put(ConstVal.SERVICE_PATH, srcPath + "/" + packageInfo.get(ConstVal.SERVICE).replaceAll("\\.", StringPool.BACK_SLASH + File.separator));
//        pathInfo.put(ConstVal.SERVICE_IMPL_PATH, srcPath + "/" + packageInfo.get(ConstVal.SERVICE_IMPL).replaceAll("\\.", StringPool.BACK_SLASH + File.separator));
//
//        pc.setPathInfo(pathInfo);
//        mpg.setPackageInfo(pc);
//
//        //自定义配置
//        InjectionConfig cfg = new InjectionConfig() {
//            @Override
//            public void initMap() {
//                //to do nothing
//            }
//        };
//
//        // 配置模板
//        TemplateConfig templateConfig = new TemplateConfig();
//        //set("")表示不生成相应模块代码
//        templateConfig.setController(null);
//        // 配置自定义输出模板
//        templateConfig.setXml(null);
//        mpg.setTemplate(templateConfig);
//
//        //配置策略
//        StrategyConfig strategy = new StrategyConfig();
//        strategy.setNaming(NamingStrategy.underline_to_camel);
//        strategy.setColumnNaming(NamingStrategy.underline_to_camel);
//        //默认是false
//        strategy.setEntityLombokModel(true);
//
//        strategy.setInclude(tables);
//
//        strategy.setControllerMappingHyphenStyle(true);
//        strategy.setTablePrefix("");
////        //逻辑删除和版本号
////        strategy.setVersionFieldName("version");
////        strategy.setLogicDeleteFieldName("deleted");
////        //自动填充字段
////        TableFill fill1 = new TableFill("creator", FieldFill.INSERT);
////        TableFill fill2 = new TableFill("create_time", FieldFill.INSERT);
////        TableFill fill3 = new TableFill("modifier", FieldFill.UPDATE);
////        TableFill fill4 = new TableFill("modify_time", FieldFill.UPDATE);
////        List<TableFill> list = new ArrayList();
////        list.add(fill1);
////        list.add(fill2);
////        list.add(fill3);
////        list.add(fill4);
////        strategy.setTableFillList(list);
//        mpg.setStrategy(strategy);
//        mpg.setTemplateEngine(new FreemarkerTemplateEngine());
//        mpg.execute();
//    }
}
