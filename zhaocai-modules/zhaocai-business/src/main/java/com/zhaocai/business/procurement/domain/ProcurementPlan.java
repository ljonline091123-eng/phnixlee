package com.zhaocai.business.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购计划对象 tb_procurement_plan
 *
 * @author chenming
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_procurement_plan")
public class ProcurementPlan extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 采购计划编号
     */
    @ApiModelProperty(value = "采购计划编号")
    private String procurementPlanCode;

    /**
     * 采购计划名称
     */
    //@NotBlank(message = "采购计划名称不能为空")
    @ApiModelProperty(value = "采购计划名称")
    private String procurementPlanName;

    /**
     * 采购计划类别
     */
    //@NotNull(message = "采购计划类别不能为空")
    @ApiModelProperty(value = "采购计划类别")
    private Integer procurementPlanType;

    /**
     * 招标方式
     */
    //@NotNull(message = "招标方式不能为空")
    @ApiModelProperty(value = "招标方式")
    private Integer procurementType;

    /**
     * 项目采购层级
     */
    //@NotBlank(message = "项目采购层级不能为空")
    @ApiModelProperty(value = "项目采购层级")
    private String projectHierarchy;

    /**
     * 计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    //@NotNull(message = "计划开始时间不能为空")
    @ApiModelProperty(value = "计划开始时间")
    private Date beginDate;

    /**
     * 计划完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    //@NotNull(message = "计划完成时间不能为空")
    @ApiModelProperty(value = "计划完成时间")
    private Date endDate;

    /**
     * 计划进场时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    //@NotNull(message = "计划进场时间不能为空")
    @ApiModelProperty(value = "计划进场时间")
    private Date arrivalDate;

    /**
     * 采购填报人
     */
    @ApiModelProperty(value = "采购填报人")
    private Long procurementReporter;

    /**
     * 采购填报人名称
     */
    @ApiModelProperty(value = "采购填报人名称")
    private String procurementReporterName;

    /**
     * 采购经办人
     */
    //@NotNull(message = "采购经办人不能为空")
    @ApiModelProperty(value = "采购经办人")
    private Long procurementOfficer;

    /**
     * 采购经办人名称
     */
    //@NotNull(message = "采购经办人名称不能为空")
    @ApiModelProperty(value = "采购经办人名称")
    private String procurementOfficerName;

    /**
     * 交易标的物
     */
    @ApiModelProperty(value = "交易标的物")
    private String subjectMatter;

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
     * 价格类型 {@link com.zhaocai.business.common.enums.PriceTypeEnum}
     */
    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

    /**
     * 基价 （前端用来统一刷新列表清单的基价使用。）
     */
    @ApiModelProperty(value = "基价")
    private BigDecimal basePrice;

    /**
     * 区域-省
     */
    @ApiModelProperty(value = "区域-省")
    private String regionProvinceCode;

    /**
     * 区域-市
     */
    @ApiModelProperty(value = "区域-市")
    private String regionCityCode;

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
     * 指导价
     */
    @ApiModelProperty(value = "指导价")
    private BigDecimal guidancePrice;

    /**
     * 状态
     */
    @ApiModelProperty(value = "状态")
    private Integer state;
}
