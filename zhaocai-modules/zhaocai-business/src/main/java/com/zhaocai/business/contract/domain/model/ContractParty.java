package com.zhaocai.business.contract.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/** 合同参与方，区分采购人、供应商及其他签署方。 */
@Getter
@Setter
@ApiModel("合同参与方")
@TableName("tb_contract_party")
public class ContractParty extends BaseEntity {
    @ApiModelProperty("合同ID")
    private Long contractId;
    @ApiModelProperty("参与方类型：1采购人，2供应商，3其他")
    private Integer partyType;
    @ApiModelProperty("业务主体ID")
    private String partyId;
    @ApiModelProperty("主体名称")
    private String partyName;
    @ApiModelProperty("是否需要签署")
    private Integer needSign;
}
