package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 招标公告对象 tb_tender_notice
 * 
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_tender_notice")
public class TenderNotice extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 招标项目id */
    @ApiModelProperty(value =  "招标项目id")
    private Long projectId;

    /** 采购方案id */
    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    /** 报名截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value =  "报名截止时间")
    private Date applyTime;

    /** 联系人 */
    @ApiModelProperty(value =  "联系人")
    private String contact;

    /** 联系电话 */
    @ApiModelProperty(value =  "联系电话")
    private String phone;

    /** 联系邮箱 */
    @ApiModelProperty(value =  "联系邮箱")
    private String email;

    /** 招标公告模板id */
    @ApiModelProperty(value =  "招标公告模板id")
    private Long templateId;

    /** 招标公告附件id */
    @ApiModelProperty(value =  "招标公告附件id")
    private Long attachId;

    /** 是否设置供应商范围（0不设置 1设置） */
    @ApiModelProperty(value =  "是否设置供应商范围")
    private Integer vendorRange;

    /** 招标公告状态（招标阶段流程状态）*/
    @ApiModelProperty(value =  "招标公告状态（招标阶段流程状态）0废标/1发布/2开标/3评标/4二次洽商/5定标报告/6中标公示/7结果发布/8完成")
    private Integer noticeStatus;

    /** 是否设置开标人员（0未设置 1已设置） */
    @ApiModelProperty(value =  "是否设置开标人员（0未设置 1已设置）")
    private Integer isOpenPeople;

    @ApiModelProperty(value =  "专家是否开启评标（0否-默认 1是）")
    private Integer isEval;

//    @ApiModelProperty(value =  "是否允许调价")
//    private Long isOpenPeople;

    /**
     * 流程实例id
     */
    @ApiModelProperty(hidden = true)
    private String wfProcessId;

}
