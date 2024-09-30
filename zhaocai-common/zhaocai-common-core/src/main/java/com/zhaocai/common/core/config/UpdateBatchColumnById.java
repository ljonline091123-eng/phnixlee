package com.zhaocai.common.core.config;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.zhaocai.common.core.enums.MySqlMethod;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author ssy
 * @date 2024/8/22 20:49
 */
public class UpdateBatchColumnById extends AbstractMethod {
    private static final long serialVersionUID = 4198102405483580486L;

    private Predicate<TableFieldInfo> predicate;

    public UpdateBatchColumnById() {
        super("updateBatchColumnById");
    }

    public UpdateBatchColumnById(Predicate<TableFieldInfo> predicate) {
        super("updateBatchColumnById");
        this.predicate = predicate;
    }

    public UpdateBatchColumnById(String name, Predicate<TableFieldInfo> predicate) {
        super(name);
        this.predicate = predicate;
    }

    /**
     * 通过ID批量更新
     */
    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        MySqlMethod sqlMethod = MySqlMethod.UPDATE_BATCH_BY_ID;
        String sql = String.format(sqlMethod.getSql(), tableInfo.getTableName(), this.sqlSet(tableInfo), tableInfo.getKeyColumn(), this.sqlIn(tableInfo.getKeyProperty()));
        SqlSource sqlSource = this.languageDriver.createSqlSource(this.configuration, sql, modelClass);
        return this.addUpdateMappedStatement(mapperClass, modelClass, sqlMethod.getMethod(), sqlSource);
    }

    private String sqlSet(TableInfo tableInfo) {
        List<TableFieldInfo> fieldList = tableInfo.getFieldList();
        StringBuilder sb = new StringBuilder();

        for (TableFieldInfo fieldInfo : fieldList) {
            sb.append("<if test=\"ew.updateFields.contains(\"\").append(fieldInfo.getColumn()).append(\"\")\">")
                    .append(fieldInfo.getColumn()).append(" =\n")
                    .append("CASE ").append(tableInfo.getKeyColumn()).append("\n")
                    .append("<foreach collection=\"list\" item=\"et\" >\n")
                    .append("WHEN #{et.").append(tableInfo.getKeyProperty()).append("} THEN #{et.").append(fieldInfo.getProperty()).append("}\n")
                    .append("</foreach>\n").append("END ,\n")
                    .append("</if>\n");

        }
        return "<set>\n" + sb + "</set>";
    }

    private String sqlIn(String keyProperty) {
        StringBuilder sb = new StringBuilder();
        sb.append("<foreach collection=\"list\" item=\"et\" separator=\",\" open=\"(\" close=\")\">\n")
                .append("#{et.").append(keyProperty).append("}")
                .append("</foreach>\n");

        return sb.toString();
    }
}
