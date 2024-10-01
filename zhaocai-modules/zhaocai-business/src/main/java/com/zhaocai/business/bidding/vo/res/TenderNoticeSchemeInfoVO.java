package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @author ssy
 * @date 2024/7/3 10:32
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TenderNoticeSchemeInfoVO", description = "详情VO")
public class TenderNoticeSchemeInfoVO {

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value =  "公告id")
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value =  "报名截止时间")
    private Date applyTime;

    @ApiModelProperty(value =  "联系人")
    private String contact;

    @ApiModelProperty(value =  "联系电话")
    private String phone;

    @ApiModelProperty(value =  "联系邮箱")
    private String email;

    @ApiModelProperty(value =  "招标公告附件id")
    private Long attachId;

    @ApiModelProperty(value =  "是否设置供应商范围")
    private Integer vendorRange;

    @ApiModelProperty(value =  "招标公告状态（招标阶段流程状态）0废标/1发布/2开标/3评标/4二次洽商/5定标报告/6中标公示/7结果发布/8完成")
    private Integer noticeStatus;

    @ApiModelProperty(value =  "是否设置开标人员（0未设置 1已设置）")
    private Integer isOpenPeople;

    @ApiModelProperty(value =  "专家是否开启评标（0否-默认 1是）")
    private Integer isEval;

    @ApiModelProperty(value =  "采购方案类型（1公开招标 2邀请招标 3询价采购 4单一来源）")
    private Integer schemeType;

    @ApiModelProperty(value ="项目code")
    private  String projectCode;


}
