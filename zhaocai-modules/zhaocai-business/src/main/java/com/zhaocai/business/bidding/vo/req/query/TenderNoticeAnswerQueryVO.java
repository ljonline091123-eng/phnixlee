package com.zhaocai.business.bidding.vo.req.query;

import com.zhaocai.common.core.bean.ValidateGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

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

    @ApiModelProperty(value =  "招标公告状态（招标阶段流程状态）0废标11发布(公告)/12发布(报名情况)/1发布文件/2开标/3评标/4二次洽商/5定标报告/6中标公示/7结果发布/8完成")
    private Integer noticeStatus;

}
