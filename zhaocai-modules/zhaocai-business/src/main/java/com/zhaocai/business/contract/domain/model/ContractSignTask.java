package com.zhaocai.business.contract.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/** 合同签署任务，记录电子签平台交互状态和回调幂等键。 */
@Getter
@Setter
@ApiModel("合同签署任务")
@TableName("tb_contract_sign_task")
public class ContractSignTask extends BaseEntity {
    public static final int PENDING = 0;
    public static final int PROCESSING = 1;
    public static final int COMPLETED = 2;
    public static final int FAILED = 3;

    @ApiModelProperty("合同ID")
    private Long contractId;
    @ApiModelProperty("签署方ID")
    private Long partyId;
    @ApiModelProperty("签署平台")
    private String platform;
    @ApiModelProperty("平台合同ID")
    private String externalContractId;
    @ApiModelProperty("任务状态")
    private Integer status;
    @ApiModelProperty("最后一次回调事件ID")
    private String lastCallbackEventId;
}
