package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 易料采购合同查询返回VO
 *
 * @author lsn
 * @date 2024-10-22
 */
@Data
public class MarketMaterialContractListVO extends AdviceObject {

    @ApiModelProperty(value = "合同 id")
    private String id;

    @ApiModelProperty(value = "合同编号")
    private String agreementCode;

    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    @ApiModelProperty(value = "乙方名称")
    private String partyBName;

    @ApiModelProperty(value = "类别")
    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "procurementPlanType")
    private String expenditureBusinessType;


}
