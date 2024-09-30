package com.zhaocai.business.procurement.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

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

}
