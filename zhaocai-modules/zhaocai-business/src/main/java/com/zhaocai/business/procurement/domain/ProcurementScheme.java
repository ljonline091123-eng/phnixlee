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
 * 采购方案对象 tb_procurement_scheme
 *
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_procurement_scheme")
public class ProcurementScheme extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 项目编号
     */
    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    /**
     * 采购方案编号
     */
    @ApiModelProperty(value = "采购方案编号")
    private String procurementSchemeCode;

    /**
     * 采购方案名称
     */
    @ApiModelProperty(value = "采购方案名称")
    @NotBlank(message = "采购方案名称不能为空")
    private String procurementSchemeName;

    /**
     * 采购经办人
     */
    @ApiModelProperty(value = "采购经办人")
    private Long procurementOfficer;

    /**
     * 采购经办人名称
     */
    @ApiModelProperty(value = "采购经办人名称")
    private String procurementOfficerName;

    /**
     * 采购计划类别
     */
    @ApiModelProperty(hidden = true)
    private Integer procurementPlanType;

    /**
     * 采购方式
     */
    @ApiModelProperty(value = "采购方式")
    @NotNull(message = "采购方式不能为空")
    private Integer procurementType;

    /**
     * 上限价
     */
    @ApiModelProperty(value = "上限价")
    private BigDecimal ceilingPrice;

    /**
     * 是否收取保证金
     */
    @ApiModelProperty(value = "是否收取保证金")
    @NotNull(message = "是否收取保证金不能为空")
    private Integer isReceiveDeposit;

    /**
     * 保证金
     */
    @ApiModelProperty(value = "保证金")
    private BigDecimal securityDeposit;

    /**
     * 财务确认人员id
     */
    @NotNull(message = "财务确认人员不能为空")
    @ApiModelProperty(value = "财务确认人员id")
    private String financeConfirmId;

    /**
     * 财务确认人员名称
     */
    @NotBlank(message = "财务确认人员名称不能为空")
    @ApiModelProperty(value = "财务确认人员名称")
    private String financeConfirmName;

    /**
     * 计数方式
     */
    @ApiModelProperty(value = "计数方式")
    private Integer countingType;

    /**
     * 付款方式
     */
    @ApiModelProperty(value = "付款方式")
    private Integer paymentType;

    /**
     * 价格类型
     */
    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

    /**
     * 交易标的物编码
     */
    @ApiModelProperty(value = "交易标的物编码")
    private String subjectMatterCode;

    /**
     * 交易标的物名称
     */
    @ApiModelProperty(value = "交易标的物名称")
    private String subjectMatterName;

    /**
     * 交易标的物类型
     */
    @ApiModelProperty(value = "交易标的物类型")
    private Integer subjectMatterType;

    /**
     * 状态
     */
    @ApiModelProperty(value = "状态")
    private Integer state;

    /**
     * 流程实例id
     */
    @ApiModelProperty(hidden = true)
    private String wfProcessId;
}
