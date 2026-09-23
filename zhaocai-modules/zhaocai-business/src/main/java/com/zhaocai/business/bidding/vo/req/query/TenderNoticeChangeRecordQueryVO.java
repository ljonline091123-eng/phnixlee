package com.zhaocai.business.bidding.vo.req.query;

import com.zhaocai.common.core.bean.ValidateGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author ssy
 * @date 2024/5/31 10:17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TenderNoticeChangeRecordQueryVO", description = "招标公告变更记录VO")
public class TenderNoticeChangeRecordQueryVO {

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "招标公告状态（招标阶段流程状态）0废标11发布(公告)/12发布(报名情况)/1发布文件/2开标/3评标/4二次洽商/5定标报告/6中标公示/7结果发布/8完成")
    @NotNull(message = "招标公告状态不能为空", groups = {ValidateGroup.AddGroup.class})
    private Integer noticeStatus;

}
