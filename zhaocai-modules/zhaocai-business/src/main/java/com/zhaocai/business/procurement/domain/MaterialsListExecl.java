package com.zhaocai.business.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.annotation.Excel;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 采购物料清单对导入对象
 *
 * @author xb
 * @date 2025-09-24
 */
@Data
public class MaterialsListExecl implements Serializable {

    /**
     * 采购计划id
     */
    @Excel(name = "序号")
    private String xh;


    //@Excel(name = "物料 id")
    private String materialsId;

    /**
     * 物料清单唯一 id
     */
    private String materialsUniqueId;

    /**
     * 物料清单编码
     */
    // @NotBlank(message = "采购清单的物料清单编码不能为空")
    /*@Excel(name = "清单编码")
    private String materialsCode;*/

    /**
     * 物料清单名称
     */
    // @NotBlank(message = "采购清单的物料清单名称不能为空")
    /*@Excel(name = "清单名称")
    private String materialsName;*/

    /**
     * 物料清单名称（导入） = 商务策划:成本子目名称(导入)
     */
    @Excel(name = "清单名称（导入）")
    private String materialsNameImport;

    @Excel(name = "特征值特征项")
    private String specification;

    /**
     * 规格型号
     */
    /*@Excel(name = "规格型号")
    private String specification;

    *//**
     * 计量规则
     *//*
    @Excel(name  = "计量规则")
    private String measurementRules;

    *//**
     * 计量单位
     *//*
    @Excel(name  = "计量单位")
    private String unitMeasurement;

    *//**
     * 基本工作内容
     *//*
    @Excel(name = "工作内容")
    private String workContent;*/

    @Excel(name = "清单数量")
    private BigDecimal count;

    @Excel(name = "单价(含税)")
    private BigDecimal unitPriceInclTax;

    @Excel(name = "税率(%)")
    private BigDecimal taxRate;

    /*@Excel(name = "单价(不含税)")
    private BigDecimal unitPriceExclTax;*/

    @Excel(name = "备注")
    private String remark;

}
