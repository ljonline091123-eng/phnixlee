package com.zhaocai.business.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 采购方案-招标信息对象 tb_procurement_scheme_bidding
 *
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_procurement_scheme_bidding")
public class ProcurementSchemeBidding extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 采购方案id
     */
    @ApiModelProperty(value = "采购方案id")
    private Long schemeId;

    /**
     * 投标截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "投标截止时间")
    @NotNull(message = "投标截止时间不能为空")
    private Date bidDeadline;

    /**
     * 投标联系人
     */
    @ApiModelProperty(value = "投标联系人")
    @NotBlank(message = "不能为空")
    private String bidContactPerson;

    /**
     * 投标联系电话
     */
    @ApiModelProperty(value = "投标联系电话")
    @NotBlank(message = "投标联系电话不能为空")
    private String bidContactPhone;

    /**
     * 投标联系邮箱
     */
    @ApiModelProperty(value = "投标联系邮箱")
    @NotBlank(message = "投标联系邮箱不能为空")
    private String bidContactEmail;

    /**
     * 评分模板id
     */
    @ApiModelProperty(value = "评分模板id")
    private Long evaluationTemplateId;

    /**
     * 招标文件模板id
     */
    @ApiModelProperty(value = "招标文件附件id")
    private Long biddingAttachmentId;

    /**
     * 招标文件模板 id
     */
    @ApiModelProperty(value = "招标文件模板 id")
    private Long biddingTemplateId;

    /**
     * 合同模板id
     */
    @ApiModelProperty(value = "合同模板id")
    private Long contractTemplateId;
}
