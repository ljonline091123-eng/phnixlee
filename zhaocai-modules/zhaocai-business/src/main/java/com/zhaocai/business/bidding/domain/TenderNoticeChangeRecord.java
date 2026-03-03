package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.bean.ValidateGroup;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * 招标公告变更记录对象 tb_tender_notice_change_record
 *
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_tender_notice_change_record")
public class TenderNoticeChangeRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 招标公告id */
    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "招标公告状态（招标阶段流程状态）0废标11发布(公告)/12发布(报名情况)/1发布文件/2开标/3评标/4二次洽商/5定标报告/6中标公示/7结果发布/8完成")
    @NotNull(message = "招标公告状态不能为空", groups = {ValidateGroup.AddGroup.class})
    private Integer noticeStatus;

    /** 变更类型（1变更时间 2变更内容） */
    @ApiModelProperty(value =  "变更类型（1变更时间 2变更内容）")
    private Integer type;

    /** 变更前信息 */
    @ApiModelProperty(value =  "变更前信息")
    private String updateBefore;

    /** 变更后信息 */
    @ApiModelProperty(value =  "变更后信息")
    private String updateAfter;

    @ApiModelProperty(value = "变更后文件附件id")
    private Long attachmentId;
}
