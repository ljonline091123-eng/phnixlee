package com.zhaocai.business.agreement.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AgreementSaveVO {

    @ApiModelProperty(value = "合同 id")
    private Long id;

    @ApiModelProperty(value = "采购计划类别")
    private Integer procurementPlanType;
}
