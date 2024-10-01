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

    /** 投标截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value =  "投标截止时间")
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
    @ApiModelProperty(value =  "招标公告状态（招标阶段流程状态）0废标11发布(公告)/12发布(报名情况)/1发布文件/2开标/3评标/4二次洽商/5定标报告/6中标公示/7结果发布/8完成")
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



    /** 发布公告联系人 */
    @ApiModelProperty(value =  "发布公告联系人")
    private String contactNotice;

    /** 发布公告联系电话 */
    @ApiModelProperty(value =  "发布公告联系电话")
    private String phoneNotice;

    /** 发布公告联系邮箱 */
    @ApiModelProperty(value =  "发布公告联系邮箱")
    private String emailNotice;

    /** 发布公告报名截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value =  "发布公告报名截止时间")
    private Date applyTimeNotice;

    /** 发布公告招标公告附件id */
    @ApiModelProperty(value =  "发布公告招标公告附件id")
    private Long attachIdNotice;


    /** 二次报价截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value =  "二次报价截止时间")
    private Date twiceTime;

    /* 每开启一次二次报价，就将选中范围的投标报价对象供应商报价 复制一份并升级版本将投标报价对象twiceQuot状态打开。前端通过招标对象的版本和投标报价对象版本对比和投标报价对象二次报价开关对比进行开放是否 供应商可以报价 */
    /** 二次报价版本号，对应招标对象的版本号，如果对应不上就是在第*次开启报价时未选中或者是供应商未调价 管理端控制发版号 */
    @ApiModelProperty(value =  "二次报价版本号。从1开始")
    private Integer twiceQuotVersion;

    /** 二次报价状态，是否正在进行中，防止反复点击升级版本号 0关闭报价，1开放报价 */
    @ApiModelProperty(value =  "二次报价状态")
    private Integer twiceQuotState;


}
