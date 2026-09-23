package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 签订合同-采购方案VO
 *
 * @author chenming
 * @date 2024-06-20
 */
@Data
public class AgreementSchemeListVO extends AdviceObject {

    @ApiModelProperty(value = "采购方案 id")
    private Long schemeId;

    @ApiModelProperty(value = "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value = "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value = "采购方案名称")
    private Integer procurementPlanType;

    @ApiModelProperty(value = "类别")
    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "procurementPlanType")
    private String procurementPlanTypeText;


}
