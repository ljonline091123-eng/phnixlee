package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ssy
 * @date 2024/6/27 16:07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ExpertEvalStatusVO", description = "专家评标状态信息VO")
public class ExpertEvalStatusVO {

    @ApiModelProperty(value =  "专家id")
    private Long expertId;

    @ApiModelProperty(value =  "专家名称")
    private String expertName;

    @ApiModelProperty(value =  "评标状态（0未完成 1已完成）")
    private Integer evalStatus;

    @ApiModelProperty(value =  "评标状态（文本）")
    private String evalStatusText;

}
