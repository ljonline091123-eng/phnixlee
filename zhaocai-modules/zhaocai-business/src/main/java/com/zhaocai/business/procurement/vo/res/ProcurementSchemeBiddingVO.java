package com.zhaocai.business.procurement.vo.res;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.pub.domain.Attachment;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * 采购方案-招标信息对象 tb_procurement_scheme_bidding
 *
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_procurement_scheme_bidding")
public class ProcurementSchemeBiddingVO {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value =  "投标截止时间")
    private Date bidDeadline;

    @ApiModelProperty(value =  "投标联系人")
    private String bidContactPerson;

    @ApiModelProperty(value =  "投标联系电话")
    private String bidContactPhone;

    @ApiModelProperty(value =  "投标联系邮箱")
    private String bidContactEmail;

    @ApiModelProperty(value =  "评标方法")
    private Integer evaluationMethod;

    @ApiModelProperty(value =  "评分模板")
    private ProcurementSchemeTemplateVO evaluationTemplate;

    @ApiModelProperty(value =  "招标文件模板")
    private ProcurementSchemeTemplateVO biddingTemplate;

    @ApiModelProperty(value =  "合同模板")
    private ProcurementSchemeTemplateVO contractTemplate;

    @ApiModelProperty(value =  "其他文件")
    private ProcurementSchemeOtherFileVO otherFile;

    /**
     * 其他文件模板id（招标公告、招标方案、招标控制价）
     */
    @ApiModelProperty(value = "其他文件模板list")
    private List<Attachment> otherAttachmentList;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value =  "发布公告报名截止时间")
    private Date applyTimeNotice;

    @ApiModelProperty(value =  "招标公告附件")
    private ProcurementSchemeTemplateVO noticeAttachment;
}
