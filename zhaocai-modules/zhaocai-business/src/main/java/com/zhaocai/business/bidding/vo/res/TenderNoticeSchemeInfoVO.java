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
