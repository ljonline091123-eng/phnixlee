package com.zhaocai.business.procurement.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购方案vo
 *
 * @author chenming
 * @date 2024/06/01
 */
@Data
public class ProcurementSchemeVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value = "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value = "采购经办人")
    private Long procurementOfficer;

    @ApiModelProperty(value = "采购经办人名称")
    private String procurementOfficerName;

    @ApiModelProperty(hidden = true)
    private Integer procurementPlanType;

    @ApiModelProperty(value = "上限价")
    private BigDecimal ceilingPrice;

    @ApiModelProperty(hidden = true)
    private Integer procurementType;

    @ApiModelProperty(value = "是否收取保证金")
    private Integer isReceiveDeposit;

    @ApiModelProperty(value = "保证金")
    private BigDecimal securityDeposit;

    @ApiModelProperty(value = "财务确认人员id")
    private String financeConfirmId;

    @ApiModelProperty(value = "财务确认人员名称")
    private String financeConfirmName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "状态")
    private Integer state;

    @ApiModelProperty(value = "流程 id")
    private String wfProcessId;

    @ApiModelProperty(value = "计数方式")
    private Integer countingType;

    @ApiModelProperty(value = "付款方式")
    private Integer paymentType;

    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

    @ApiModelProperty(value = "交易标的物编码")
    private String subjectMatterCode;

    @ApiModelProperty(value = "交易标的物名称")
    private String subjectMatterName;

    @ApiModelProperty(value = "交易标的物类型")
    private Integer subjectMatterType;

    @ApiModelProperty(value = "项目对应的接口id")
    private Long projectDeptId;

    @DictCache(dictBizEnum = DictBizEnum.IS_RECEIVE_DEPOSIT,filedName = "isReceiveDeposit")
    @ApiModelProperty(value = "是否收取保证金-文本")
    private String isReceiveDepositText;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_TYPE,filedName = "procurementType")
    @ApiModelProperty(value = "采购方式-文本")
    private String procurementTypeText;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "procurementPlanType")
    @ApiModelProperty(value = "采购类别-文本")
    private String procurementPlanTypeText;

    @MoneyFormat(filedName = "ceilingPrice",scale = 2)
    @ApiModelProperty(value = "上限价")
    private String ceilingPriceText;

    @MoneyFormat(filedName = "securityDeposit",scale = 2)
    @ApiModelProperty(value = "保证金")
    private String securityDepositText;

    @DictCache(dictBizEnum = DictBizEnum.PRICE_TYPE,filedName = "priceType")
    @ApiModelProperty(value = "价格类型-文本")
    private String priceTypeText;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_COUNTING_TYPE,filedName = "countingType")
    @ApiModelProperty(value = "计数方式")
    private String countingTypeText;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PAYMENT_TYPE,filedName = "paymentType")
    @ApiModelProperty(value = "付款方式")
    private String paymentTypeText;

    @ApiModelProperty(value = "交易标的物")
    private String subjectMatterText;
}
