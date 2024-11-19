package com.zhaocai.business.procurement.vo.res;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.cache.DictBizCache;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.enums.PriceTypeEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 采购计划拆分合约
 *
 * @author chenming
 * @date 2024/06/15
 */
@Data
public class ProcurementPlanContractSplitVO extends AdviceObject {

    @ApiModelProperty(value =  "采购计划id")
    private Long planId;

    @ApiModelProperty(value =  "采购计划名称")
    private String procurementPlanName;

    @ApiModelProperty(value =  "项目采购层级")
    private String projectHierarchy;

    @ApiModelProperty(value = "拆分合约规划 id")
    private Long splitContractId;

    @ApiModelProperty(value = "拆分合约规划名称")
    private String splitContractName;

    @ApiModelProperty(value = "拟签约合同拆包范围")
    private String contractScope;

    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

    @ApiModelProperty(value = "采购计划类别")
    private Integer procurementPlanType;

    @ApiModelProperty(value = "采购方式")
    private Integer procurementType;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "procurementPlanType")
    @ApiModelProperty(value = "采购计划类别")
    private String procurementPlanTypeText;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_TYPE,filedName = "procurementType")
    @ApiModelProperty(value = "采购方式")
    private String procurementTypeText;

    @ApiModelProperty(value = "价格类型")
    private String priceTypeText;

    public String getPriceTypeText() {
        if (PriceTypeEnum.FLOAT_PRICE.equalsType(this.getPriceType())) {
            this.priceTypeText = "浮动价";
        } else if (PriceTypeEnum.FIXED_FLOAT_PRICE.equalsType(this.getPriceType())) {
            this.priceTypeText = "固定、浮动价";
        }else{
            this.priceTypeText = "/";
        }
        return priceTypeText;
    }

}
