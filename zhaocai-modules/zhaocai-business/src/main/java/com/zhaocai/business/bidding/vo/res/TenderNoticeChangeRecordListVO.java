package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.bean.ValidateGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * @author ssy
 * @date 2024/5/31 10:18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TenderNoticeChangeRecordListVO", description = "招标公告变更记录VO")
public class TenderNoticeChangeRecordListVO implements Serializable {
    private static final long serialVersionUID = -3813907374934882636L;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "招标公告状态（招标阶段流程状态）0废标11发布(公告)/12发布(报名情况)/1发布文件/2开标/3评标/4二次洽商/5定标报告/6中标公示/7结果发布/8完成")
    @NotNull(message = "招标公告状态不能为空", groups = {ValidateGroup.AddGroup.class})
    private Integer noticeStatus;

    @ApiModelProperty(value =  "变更类型（1变更时间 2变更内容）")
    private Integer type;

    @ApiModelProperty(value =  "变更类型（文本）")
    private String typeText;

    @ApiModelProperty(value =  "变更前信息")
    private String updateBefore;

    @ApiModelProperty(value =  "变更后信息")
    private String updateAfter;

    @ApiModelProperty(value = "创建者")
    private String createBy;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;

}
