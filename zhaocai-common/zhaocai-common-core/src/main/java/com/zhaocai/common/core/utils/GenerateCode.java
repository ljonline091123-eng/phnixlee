package com.zhaocai.common.core.utils;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.converts.DmTypeConvert;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.zhaocai.common.core.web.domain.BaseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.util.*;

/**
 * ssy
 * 生成代码工具 生成的位置在工程路径下的 genrate文件夹下
 */
public class GenerateCode {

    @Resource
    JdbcTemplate jdbcTemplate;

    // url
    private static final String url =
//        "jdbc:dm://10.255.102.23:5236/DPA?zeroDateTimeBehavior=convertToNull&useUnicode=true&characterEncoding=utf-8";
        "jdbc:mysql://192.168.6.151:3306/hnjiantou-zhaocai-dev?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8";


    // 数据库用户名
    private static final String userName = "root";
    // 数据库密码
    private static final String passWord = "WanHeng@2024";
    // 自定义选择模块名
    private static final String moduleName = "receipt";

    public static void main(String[] args) {

        /*
        这里定义要生成的表名
        */

        List<String> tables = new ArrayList<>();
        tables.add("tb_market_materials_list");

        DataSourceConfig.Builder dataSourceConfigBuilder =
            new DataSourceConfig.Builder(url, userName, passWord).typeConvert((globalConfig, fieldType) -> {
                String str = fieldType.toLowerCase();
                if (str.equals("datetime")) {
                    return DbColumnType.DATE;
                } else if (str.equals("bigint")) {
                    return DbColumnType.LONG;
                } else if (str.equals("tinyint")) {
                    return DbColumnType.INTEGER;
                } else if (str.equals("date")) {
                    return DbColumnType.LOCAL_DATE;
                } else if (str.equals("timestamp")) {
                    return DbColumnType.LOCAL_DATE_TIME;
                } else if (str.matches("number\\(1[0-9]\\)")) {
                    return DbColumnType.LONG;
                } else if (str.equals("number(1)")) {
                    return DbColumnType.BOOLEAN;
                } else {
                    // 其他类型采用默认
                    return new DmTypeConvert().processTypeConvert(globalConfig, fieldType);
                }
            }); // 原来的DMQuery不知道怎么会出现重复列;

        FastAutoGenerator.create(dataSourceConfigBuilder).globalConfig(builder -> {
            builder.author("susiyuan") // 作者
                .outputDir(System.getProperty("user.dir") + "/zhaocai-modules/zhaocai-business/src/main/java") // 输出路径(写到java目录)
                // .disableOpenDir()
                .enableSwagger() // 开启swagger
                .commentDate("yyyy-MM-dd").fileOverride(); // 开启覆盖之前生成的文件

        }).packageConfig(builder -> {
            builder.parent("com.zhaocai.business").moduleName(moduleName).entity("domain").service("service")
                .serviceImpl("service.impl").controller("controller").mapper("mapper").xml("mapper")
                .pathInfo(Collections.singletonMap(OutputFile.mapperXml,
                    System.getProperty("user.dir") + "/zhaocai-modules/zhaocai-business/src/main/resources/mapper/" + moduleName));
        }).strategyConfig(builder -> {
            builder.addInclude(tables).addTablePrefix("tb_", "sys_") // 表前綴
                .serviceBuilder().formatServiceFileName("I%sService").formatServiceImplFileName("%sServiceImpl")
                .entityBuilder()
                .addSuperEntityColumns("create_by", "create_time", "update_by", "update_time", "del_flag")
                .enableLombok().logicDeleteColumnName("del_flag").superClass(BaseEntity.class) // 实体类父类
                .enableTableFieldAnnotation() // 开启生成 TableField 注解（注释掉的時候主键要手动添加注释）
                .controllerBuilder().formatFileName("%sController").enableRestStyle().mapperBuilder()
                .enableBaseResultMap() // 生成通用的resultMap
                .superClass(BaseMapper.class).formatMapperFileName("%sMapper").enableMapperAnnotation()
                .formatXmlFileName("%sMapper").enableBaseColumnList(); // 生成基础列;;
        })
//                .injectionConfig(consumer -> {
//            Map<String, String> customFile = new HashMap<>();
//            // 配置DTO（需要的话）但是需要有能配置Dto的模板引擎，比如freemarker，但是这里我们用的VelocityEngine，因此不多作介绍
//            customFile.put("DTO.java", "/templates/entityDTO.java.ftl");
//            consumer.customFile(customFile);
//           })
           .execute();
    }
}
