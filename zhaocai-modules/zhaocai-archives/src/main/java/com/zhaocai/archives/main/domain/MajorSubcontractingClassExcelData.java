package com.zhaocai.archives.main.domain;

import com.zhaocai.common.core.annotation.Excel;
import lombok.Data;

import java.io.Serializable;


@Data
public class MajorSubcontractingClassExcelData implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "序号")
    private String xh;

    /**
     * 父节点
     */
    @Excel(name = "*父级专业分包分类编码")
    private String parentCode;

    /**
     * 专业分包分类编码
     */
    @Excel(name = "*专业分包分类编码")
    private String majorSubcontractingClassCode;

    /**
     * 专业分包分类名称
     */
    @Excel(name = "*专业分包分类名称")
    private String majorSubcontractingClassName;

    /**
     * 计量单位
     */
    @Excel(name = "计量单位")
    private String measureUnit;

    /**
     * 特征项编号
     */
    @Excel(name = "特征项编号")
    private String featureCode;

    /**
     * 特征项名称
     */
    @Excel(name = "特征项名称")
    private String featureName;


    /**
     * 特征值编号
     */
    @Excel(name = "特征值编号")
    private String featureValueCode;

    /**
     * 特征值名称
     */
    @Excel(name = "特征值名称")
    private String featureValueName;

    /**
     * 专业分包编号
     */
    @Excel(name = "专业分包编号")
    private String majorSubcontractingCode;

    /**
     * 专业分包名称
     */
    @Excel(name = "专业分包名称")
    private String majorSubcontractingName;

    /**
     * 特征项及特征值
     */
    @Excel(name = "特征项及特征值")
    private String feature;

    /**
     * 计量单位
     */
    @Excel(name = "单位")
    private String unit;

    /**
     * 规格
     */
    @Excel(name = "规格")
    private String specs;

    /**
     * 计量规则
     */
    @Excel(name = "计量规则")
    private String metrologicalRules;

    /**
     * 基本工作内容
     */
    @Excel(name = "基本工作内容")
    private String basicJob;


}
