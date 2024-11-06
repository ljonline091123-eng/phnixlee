package com.zhaocai.business.bidding.vo.res;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ContractPlanningNoticeVO", description = "招标关联的合约规划对象")
public class ContractPlanningNoticeVO {

    @ApiModelProperty(value = "合约拆分id")
    private Long splitContractId;

    @ApiModelProperty(value = "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value = "招标id")
    private Long noticeId;

    @ApiModelProperty(value = "采购计划id")
    private Long planId;

    @ApiModelProperty(value =  "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value = "合约规划编码")
    private String contractPlanningCode;

    @ApiModelProperty(value = "合约规划id")
    private String contractPlanningId;

    @ApiModelProperty(value = "合约拆分名称")
    private String splitContractName;

    @ApiModelProperty(value = "合约规划名称")
    private String contractPlanningName;



    @ApiModelProperty("推送状态（0未推送 1已推送）")
    private Integer pushStatus;
}
