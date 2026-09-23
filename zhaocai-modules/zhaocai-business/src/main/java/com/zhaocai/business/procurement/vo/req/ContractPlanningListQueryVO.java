package com.zhaocai.business.procurement.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 项目合约规划
 *
 * @author chenming
 * @date 2024/05/27
 */
@Data
@ApiModel(value = "项目合约规划")
public class ContractPlanningListQueryVO extends PageRecive {

    @ApiModelProperty(value = "项目合约名称")
    private String contractName;

    @ApiModelProperty(value = "合约规划类型")
    private Integer contractType;

    @NotBlank(message = "项目 id 不能为空")
    @ApiModelProperty(value = "项目 id")
    private String projectId;

    @ApiModelProperty(value = "招标方式")
    private String biddingMethodCode;

    @ApiModelProperty(value = "招标责任单位")
    private String bidResponsibleOrg;

    @ApiModelProperty(value = "招标状态")
    private String biddingState;
}
