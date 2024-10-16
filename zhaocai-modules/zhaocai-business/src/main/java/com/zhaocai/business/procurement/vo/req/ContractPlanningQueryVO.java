package com.zhaocai.business.procurement.vo.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 项目合约规划
 *
 * @author chenming
 * @date 2024/05/27
 */
@Data
@ApiModel(value = "项目合约规划")
public class ContractPlanningQueryVO {

    public ContractPlanningQueryVO(List<String> contractIdList) {
        this.contractIdList = contractIdList;
    }

    /**
     * 合约规划编码
     */
    @ApiModelProperty(value = "合约规划编码")
    private String contractPlanningCode;

    /**
     * 合约规划id
     */
    @ApiModelProperty(value = "合约规划id")
    private String contractPlanningId;

    @ApiModelProperty(value = "合约规划id")
    private List<String> contractIdList;

}
