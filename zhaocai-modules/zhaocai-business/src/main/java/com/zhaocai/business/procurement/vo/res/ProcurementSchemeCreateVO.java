package com.zhaocai.business.procurement.vo.res;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购方案创建
 *
 * @author chenming
 * @date 2024-06-19
 */
@Data
public class ProcurementSchemeCreateVO extends AdviceObject {

    @ApiModelProperty(value = "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value = "采购类型")
    private Integer procurementPlanType;

    @ApiModelProperty(value = "采购经办人")
    private String procurementOfficerName;

    @ApiModelProperty(value = "上限价")
    private BigDecimal ceilingPrice;

    @ApiModelProperty(value = "项目所对应的机构 id")
    private Long projectDeptId;

    @ApiModelProperty(value = "交易标的物类型")
    private Integer subjectMatterType;

    @ApiModelProperty(value = "交易标的物名称")
    private String subjectMatterName;

    @ApiModelProperty(value = "交易标的物编码")
    private String subjectMatterCode;

    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

    @ApiModelProperty(value = "计数方式")
    private Integer countingType;

    @ApiModelProperty(value = "付款方式")
    private Integer paymentType;

    @JsonIgnore
    private Long firstProcurementPlanId;

    @JsonIgnore
    private String projectCode;

    @DictCache(dictBizEnum = DictBizEnum.PRICE_TYPE,filedName = "priceType")
    @ApiModelProperty(value = "价格类型-文本")
    private String priceTypeText;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_COUNTING_TYPE,filedName = "countingType")
    @ApiModelProperty(value = "计数方式")
    private String countingTypeText;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PAYMENT_TYPE,filedName = "paymentType")
    @ApiModelProperty(value = "付款方式")
    private String paymentTypeText;
}
