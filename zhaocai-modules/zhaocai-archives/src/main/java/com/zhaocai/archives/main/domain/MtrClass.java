package com.zhaocai.archives.main.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.annotation.Excel;
import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 材料分类主对象 mtr_class
 *
 * @author lzq
 * @date 2025-01-06
 */
@Data
@TableName(value = "mtr_class")
public class MtrClass implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 未推送到中台
     */
    public static final String WTS = "0";

    /**
     * 已推送到中台
     */
    public static final String YTS = "1";

    /**
     * 有更新许推送到中台
     */
    public static final String XGTS = "2";

    /**
     * 有删除需要推送到中台
     */
    public static final String SCTS = "3";

    /**
     * 启用数据（貌似用不到）
     */
    public static final String QYTS = "4";

    /**
     * $column.columnComment
     */
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    /**
     * 材料分类编码
     */
    @Excel(name = "材料分类编码")
    private String mtrClassCode;

    /**
     * 材料分类名称
     */
    @Excel(name = "材料分类名称")
    private String mtrClassName;

    /**
     * 材料分类下级类型 1特征项 2具体材料
     */
    @Excel(name = "材料分类下级类型 1特征项 2具体材料")
    private String mtrClassType;

    /**
     * 计量单位
     */
    @Excel(name = "计量单位")
    private String measureUnit;

    /**
     * 是否交易标的物 true是 false不是
     */
    @Excel(name = "是否交易标的物 true是 false不是")
    private Integer subjectMatter;

    /**
     * 父节点
     */
    @Excel(name = "父节点")
    private String parentId;

    /**
     * 分类层级
     */
    @Excel(name = "分类层级")
    private String classLevel;

    /**
     * 分类层级
     */
    @Excel(name = "分类层级")
    private String classLevelCd;

    /**
     * 创建者id
     */
    @Excel(name = "创建者id")
    private String createId;

    /**
     * 创建者
     */
    @Excel(name = "创建者")
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新者
     */
    @Excel(name = "更新者")
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;


    /**
     * $column.columnComment
     */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long valid;

    /**
     * 副库id
     */
    @Excel(name = "副库id")
    private Long sonId;


    /**
     * 所属层级
     */
    @TableField(exist = false)
    private String belongingLevel;

    /**
     * 是否同步中台0否1是2需要更新
     */
    @Excel(name = "是否同步中台")
    private String isTb;

}
