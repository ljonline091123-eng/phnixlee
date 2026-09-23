package com.zhaocai.business.procurement.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author ssy
 * @date 2024/9/12 14:06
 */
@Data
public class ProcurementPlanPushVO {

    @ApiModelProperty(value = "推送第三方用户集合")
    private List<ProcurementPlanPushUserVO> userList;

    @ApiModelProperty(value = "合约规划名称")
    private String contractPlanningName;

    @ApiModelProperty(value = "项目合约id")
    private String contractPlanningId;

    @ApiModelProperty(value = "项目合约编码")
    private String contractPlanningCode;

    @ApiModelProperty(value =  "第三方待办跳转地址")
    private String redirectUrl;


    /**
     * 新增一些字段
     * Time:2024/10/18 下午4:56
     * */

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

    @ApiModelProperty(value = "招标时间")
    private String biddingTime;

    @ApiModelProperty(value = "进场时间")
    private String enterIntoTime;

    @ApiModelProperty(value = "项目code")
    private String projectCode;

}
