package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author ssy
 * @date 2024/5/28 18:00
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "VendorNoticeListVO", description = "供应商可查看的招标公告列表数据VO")
public class VendorNoticeListVO {

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "投标单id")
    private Long biddingInfoId;

    @ApiModelProperty(value =  "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value =  "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value =  "招标单位")
    private String unit;

    @ApiModelProperty(value =  "联系人")
    private String contact;

    @ApiModelProperty(value =  "联系电话")
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间|发布时间")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value =  "投标截止时间")
    private Date applyTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value =  "报名截止时间")
    private Date applyTimeNotice;

    @ApiModelProperty(value =  "招标公告状态（招标阶段流程状态）1发布/2开标/3评标/4二次洽商/5定标报告/6中标公示/7结果发布")
    private Integer noticeStatus;

    @ApiModelProperty(value =  "招标公告状态文本")
    private String noticeStatusText;

    @ApiModelProperty(value =  "是否投标（未投标|已投标）")
    private String bidStatus;

    @ApiModelProperty(value =  "报名单id")
    private Long tenderApplyId;

    @ApiModelProperty(value =  "是否报名（未报名|已报名）")
    private String tenderApplyStatus;

    /** 发布公告招标公告附件id */
    @ApiModelProperty(value =  "发布公告招标公告附件id")
    private Long attachIdNotice;

    @ApiModelProperty(value =  "投标公告附件")
    private AttachmentVO attachmentNotice;

    @ApiModelProperty(value =  "二次报价设置（默认0关 1开）")
    private Integer twiceQuot;

    @ApiModelProperty(value =  "最小核算项目名称（多个项目以，隔开）")
    private String minProjectName;

    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

    @ApiModelProperty(value = "交易标的物")
    private Integer subjectMatterType;

}
