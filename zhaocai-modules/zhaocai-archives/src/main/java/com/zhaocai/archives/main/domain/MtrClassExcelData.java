package com.zhaocai.archives.main.domain;

import com.zhaocai.common.core.annotation.Excel;
import lombok.Data;

import java.io.Serializable;

/**
 * 材料档案导入
 *
 * @author lzq
 * @date 2025-06-13
 */
@Data
public class MtrClassExcelData implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "序号")
    private String xh;


    /**
     * 父节点
     */
    @Excel(name = "*父级材料分类编码")
    private String parentCode;


    /**
     * 材料分类编码
     */
    @Excel(name = "*材料分类编码")
    private String mtrClassCode;


    /**
     * 材料分类名称
     */
    @Excel(name = "*材料分类名称")
    private String mtrClassName;


    /**
     * 计量单位
     */
    @Excel(name = "分类单位")
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
     * 材料编号
     */
    @Excel(name = "材料编号")
    private String mtrCode;

    /**
     * 材料名称
     */
    @Excel(name = "材料名称")
    private String mtrName;

    /**
     * 规格
     */
    @Excel(name = "规格")
    private String specs;

    /**
     * 计量单位
     */
    @Excel(name = "单位")
    private String unit;


}
