package com.zhaocai.business.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 采购物料清单对象 tb_materials_list
 *
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_materials_list")
public class MaterialsList extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 采购计划id
     */
    @ApiModelProperty(value = "采购计划id")
    private Long planId;

    /**
     * 合约采购拆分id
     */
    @ApiModelProperty(value = "合约采购拆分id")
    private Long contractSplitId;

    /**
     * 物料 id {@link com.zhaocai.business.manager.http.service.ContractPlanService#getContractMaterialsList}
     */
    @NotBlank(message = "采购清单的物料 id不能为空")
    @ApiModelProperty(value = "物料 id")
    private String materialsId;

    /**
     * 物料清单唯一 id
     */
    @ApiModelProperty(value = "物料清单唯一 id")
    private String materialsUniqueId;

    /**
     * 物料清单编码
     */
    @NotBlank(message = "采购清单的物料清单编码不能为空")
    @ApiModelProperty(value = "物料清单编码")
    private String materialsCode;

    /**
     * 物料清单名称
     */
    @NotBlank(message = "采购清单的物料清单名称不能为空")
    @ApiModelProperty(value = "物料清单名称")
    private String materialsName;

    /**
     * 交易标的物编码
     */
    @NotBlank(message = "交易标的物编码不能为空")
    @ApiModelProperty(value = "交易标的物编码")
    private String subjectMatterCode;

    /**
     * 交易标的物名称
     */
    @NotBlank(message = "交易标的物名称不能为空")
    @ApiModelProperty(value = "交易标的物名称")
    private String subjectMatterName;

    /**
     * 交易标的物标识
     */
    @NotBlank(message = "交易标的物标识不能为空")
    @ApiModelProperty(value = "交易标的物标识")
    private String subjectMatterFlag;

    /**
     * 规格型号
     */
    @ApiModelProperty(value = "规格型号")
    private String specification;

    /**
     * 计量规则
     */
    @ApiModelProperty(value = "计量规则")
    private String measurementRules;

    /**
     * 计量单位
     */
    @ApiModelProperty(value = "计量单位")
    private String unitMeasurement;

    /**
     * 基本工作内容
     */
    @ApiModelProperty(value = "基本工作内容")
    private String workContent;

    /**
     * 成本科目档案 id
     */
    @ApiModelProperty(value = "成本科目档案 id")
    private String costAccountId;

    /**
     * 成本科目编码
     */
    @NotBlank(message = "采购清单的成本科目编码不能为空")
    @ApiModelProperty(value = "成本科目编码")
    private String costAccountCode;

    /**
     * 成本科目名称
     */
    @NotBlank(message = "采购清单的成本科目名称不能为空")
    @ApiModelProperty(value = "成本科目名称")
    private String costAccountName;

    /**
     * 数量
     */
    @ApiModelProperty(value = "数量")
    private BigDecimal count;

    /**
     * 已使用数量
     */
    @ApiModelProperty(value = "已使用数量")
    private BigDecimal usedCount;

    /**
     * 税率
     */
    @NotNull(message = "采购清单的税率不能为空")
    @ApiModelProperty(value = "税率")
    private BigDecimal taxRate;

    /**
     * 单价(不含税)
     */
    @NotNull(message = "采购清单的单价(不含税)不能为空")
    @ApiModelProperty(value = "单价(不含税)")
    private BigDecimal unitPriceExclTax;

    /**
     * 单价(含税)
     */
    @NotNull(message = "采购清单的单价(含税) 不能为空")
    @ApiModelProperty(value = "单价(含税) ")
    private BigDecimal unitPriceInclTax;

    /**
     * 金额(不含税)
     */
    @NotNull(message = "采购清单的金额(不含税)不能为空")
    @ApiModelProperty(value = "金额(不含税)")
    private BigDecimal amountExclTax;

    /**
     * 金额(含税)
     */
    @NotNull(message = "采购清单的金额(含税)不能为空")
    @ApiModelProperty(value = "金额(含税)")
    private BigDecimal amountInclTax;

    /**
     * 税额
     */
    @NotNull(message = "采购清单的税额不能为空")
    @ApiModelProperty(value = "税额")
    private BigDecimal taxAmount;

    /**
     * 浮动价
     */
    @ApiModelProperty(value = "浮动价")
    private BigDecimal floatingPrice;

    /**
     * 卸费
     */
    @ApiModelProperty(value = "卸费")
    private BigDecimal unloadingFee;

    /**
     * 基价
     */
    @ApiModelProperty(value = "基价")
    private BigDecimal basePrice;

    /**
     * 租赁方式
     */
    @ApiModelProperty(value = "租赁方式")
    private String rentMode;

    /**
     * 租赁时间
     */
    @ApiModelProperty(value = "租赁时间")
    private BigDecimal rentTime;

    /**
     * 租赁数量
     */
    @ApiModelProperty(value = "租赁数量")
    private BigDecimal rentQuantity;

    /**
     * 价格类型
     */
//    @NotNull(message = "采购清单的价格类型不能为空")
    @ApiModelProperty(value = "价格类型")
    private Integer priceType;
}
