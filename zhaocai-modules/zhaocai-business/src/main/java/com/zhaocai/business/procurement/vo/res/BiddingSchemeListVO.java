package com.zhaocai.business.procurement.vo.res;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * @author ssy
 * @date 2024/6/6 19:09
 */
@Data
public class BiddingSchemeListVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value = "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value = "采购计划类别")
    private Integer procurementPlanType;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "procurementPlanType")
    @ApiModelProperty(value = "采购计划类别-文本 ")
    private String procurementPlanTypeText;

    @ApiModelProperty(value = "采购方式")
    private Integer procurementType;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_TYPE,filedName = "procurementType")
    @ApiModelProperty(value = "采购方式-文本")
    private String procurementTypeText;

    @ApiModelProperty(value = "采购经办人名称")
    private String procurementOfficerName;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "状态")
    private Integer state;

    @ApiModelProperty(value = "状态-文本")
    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_STATE,filedName = "state")
    private String stateText;

    @ApiModelProperty(value =  "招标文件模板")
    private ProcurementSchemeTemplateVO biddingTemplate;

    @ApiModelProperty(value =  "投标联系人")
    private String bidContactPerson;

    @ApiModelProperty(value =  "投标联系电话")
    private String bidContactPhone;

    @ApiModelProperty(value =  "投标联系邮箱")
    private String bidContactEmail;

    @ApiModelProperty(value =  "招标公告状态（招标阶段流程状态）")
    private Integer noticeStatus;

    @ApiModelProperty(value =  "招标公告状态文本")
    private String noticeStatusText;

    @ApiModelProperty(value =  "招标公告id）")
    private Long noticeId;


    @ApiModelProperty(value =  "项目编码")
    private String projectCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value =  "投标截止时间")
    private Date bidDeadline;

    @ApiModelProperty(value =  "创建人id")
    private Long createId;

    @ApiModelProperty(value =  "是否为采购经办人")
    private Boolean purchaseOfficer;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value =  "发布公告报名截止时间")
    private Date applyTimeNotice;

    @ApiModelProperty(value =  "招标公告附件")
    private ProcurementSchemeTemplateVO noticeAttachment;
}
