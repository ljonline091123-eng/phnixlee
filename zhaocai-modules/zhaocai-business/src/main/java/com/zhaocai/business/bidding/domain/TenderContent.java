package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 招标内容（已弃用）对象 tb_tender_content
 * 
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_tender_content")
public class TenderContent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 投标截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value =  "投标截止时间")
    private Date biddingTime;

    /** 联系人 */
    @ApiModelProperty(value =  "联系人")
    private String contact;

    /** 联系电话 */
    @ApiModelProperty(value =  "联系电话")
    private String phone;

    /** 联系邮箱 */
    @ApiModelProperty(value =  "联系邮箱")
    private String email;

    /** 招标文件附件id */
    @ApiModelProperty(value =  "招标文件附件id")
    private Long tenderAttachId;

    /** 评标规则附件id */
    @ApiModelProperty(value =  "评标规则附件id")
    private Long evaluatAttachId;

    /** 合同文件附件id */
    @ApiModelProperty(value =  "合同文件附件id")
    private Long contractAttachId;

    /** 招标项目id */
    @ApiModelProperty(value =  "招标项目id")
    private Long projectId;

    /** 采购方案id */
    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    /** 二次报价截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value =  "二次报价截止时间")
    private Date twiceBiddingTime;
}
