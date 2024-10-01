package com.zhaocai.business.bidding.vo.req.query;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ssy
 * @date 2024/5/30 16:45
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TenderNoticeAnswerQueryVO", description = "查询招标公告答疑列表VO")
public class TenderNoticeAnswerQueryVO {

    @ApiModelProperty(value =  "关联业务id")
    private Long busId;

    @ApiModelProperty(value =  "提问用户")
    private Long questionUser;

    @ApiModelProperty(value =  "查询未答疑或已答疑用户（0未答疑 1已答疑）")
    private Integer answerType;

}
