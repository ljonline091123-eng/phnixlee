package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.bidding.domain.TenderApply;
import com.zhaocai.business.bidding.domain.TenderNotice;
import com.zhaocai.business.bidding.domain.TenderNoticeRange;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * @author ssy
 * @date 2024/5/30 14:32
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TenderNoticeDetailVO", description = "招标公告详情VO")
public class TenderNoticeDetailVO {

    @ApiModelProperty(value =  "招标公告信息")
    private TenderNotice tenderNotice;

    @ApiModelProperty(value =  "招标公告供应商范围")
    private List<TenderNoticeRange> rangeList;

    @ApiModelProperty(value =  "是否投标（0未投标 1已投标）")
    private Integer bidStatus;

    @ApiModelProperty(value =  "是否报名（未报名 已报名）")
    private String applyStatus;

    @ApiModelProperty(value =  "是否投标时间结束（0未结束 1已结束）")
    private Integer bidEndStatus;

    @ApiModelProperty(value =  "招标公告状态文本")
    private String noticeStatusText;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value =  "投标截止时间")
    private Date bidEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value =  "报名截止时间")
    private Date applyTimeNotice;

    @ApiModelProperty(value =  "投标截止时间戳")
    private Long endTimeStamp;

    @ApiModelProperty(value =  "投标公告附件")
    private AttachmentVO attachmentNotice;

    @ApiModelProperty(value =  "投标公告报名供应商列表")
    private List<TenderApply> tenderApplyList;

    @ApiModelProperty(value =  "投标公告附件")
    private List<AttachmentVO> attachmentList;

    @ApiModelProperty(value =  "定标附件")
    private List<AttachmentVO> calibrationAttachmentList;

    /* 弃用 */
    @ApiModelProperty(value =  "二次报价设置（默认0关 1开）")
    private Integer twiceQuot;

    /* 弃用 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value =  "二次报价截止时间")
    private Date twiceTime;

    @ApiModelProperty(value =  "投标单id")
    private Long biddingInfoId;

    @ApiModelProperty(value =  "采购方案类型（1公开招标 2邀请招标 3询价采购 4单一来源）")
    private Integer schemeType;

    @ApiModelProperty(value =  "是否为财务确认人员")
    private Boolean financeConfirmUser;

    @ApiModelProperty(value =  "是否为开标待办人员")
    private Boolean openTodoUser;

    @ApiModelProperty(value =  "是否为采购经办人")
    private Boolean purchaseOfficer;

/*    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value =  "当前时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date curTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    @ApiModelProperty(value =  "投标截止时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private Date endApplyTime;*/

}
