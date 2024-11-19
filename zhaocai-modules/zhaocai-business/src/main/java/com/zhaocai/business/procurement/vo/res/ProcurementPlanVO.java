package com.zhaocai.business.procurement.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购计划对象 tb_procurement_plan
 *
 * @author chenming
 * @date 2024-05-24
 */
@Data
public class ProcurementPlanVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    @ApiModelProperty(value = "项目名称")
    private String projectName;

    @ApiModelProperty(value = "采购计划编号")
    private String procurementPlanCode;

    @ApiModelProperty(value = "采购计划名称")
    private String procurementPlanName;

    @ApiModelProperty(value = "采购计划类别")
    private Integer procurementPlanType;

    @ApiModelProperty(value = "招标方式")
    private Integer procurementType;

    @ApiModelProperty(value = "项目采购层级")
    private String projectHierarchy;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "计划开始时间")
    private Date beginDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "计划完成时间")
    private Date endDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "计划进场时间")
    private Date arrivalDate;

    @ApiModelProperty(value = "采购填报人")
    private Long procurementReporter;

    @ApiModelProperty(value = "采购填报人名称")
    private String procurementReporterName;

    @ApiModelProperty(value = "采购经办人")
    private Long procurementOfficer;

    @ApiModelProperty(value = "采购经办人名称")
    private String procurementOfficerName;

    @ApiModelProperty(value = "状态")
    private Integer state;

    @ApiModelProperty(value = "交易标的物名称")
    private String subjectMatterName;

    @ApiModelProperty(value = "交易标的物类型")
    private Integer subjectMatterType;

    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

    /** 基价 （前端用来统一刷新列表清单的基价使用。） */
    @ApiModelProperty(value = "基价")
    private BigDecimal basePrice;

    @ApiModelProperty(value = "区域-省")
    private String regionProvinceCode;

    @ApiModelProperty(value = "区域-市")
    private String regionCityCode;

    @ApiModelProperty(value = "区域-省")
    private String regionProvinceName;

    @ApiModelProperty(value = "区域-市")
    private String regionCityName;

    @ApiModelProperty(value = "计数方式")
    private Integer countingType;

    @ApiModelProperty(value = "付款方式")
    private Integer paymentType;

    @ApiModelProperty(value = "指导价")
    private BigDecimal guidancePrice;

    @DictCache(dictBizEnum = DictBizEnum.PRICE_TYPE,filedName = "priceType")
    @ApiModelProperty(value = "价格类型-文本")
    private String priceTypeText;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_COUNTING_TYPE,filedName = "countingType")
    @ApiModelProperty(value = "计数方式")
    private String countingTypeText;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PAYMENT_TYPE,filedName = "paymentType")
    @ApiModelProperty(value = "付款方式")
    private String paymentTypeText;

    @ApiModelProperty(value = "是否存在推送数据")
    private String isPushData;
}
