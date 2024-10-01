package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author ssy
 * @date 2024/6/25 10:02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "VendorPortalNoticeListVO", description = "招标公告列表数据VO")
public class VendorPortalNoticeListVO extends AdviceObject {

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;


    @ApiModelProperty(value =  "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value =  "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value =  "采购方式")
    private Integer procurementType;

    @ApiModelProperty(value =  "采购计划类别")
    private Integer procurementPlanType;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "procurementPlanType")
    @ApiModelProperty(value = "采购计划类别-文本 ")
    private String procurementPlanTypeText;

    @ApiModelProperty(value =  "招标单位")
    private String unit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间|发布时间")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value =  "报名截止时间")
    private Date applyTime;

    @ApiModelProperty(value =  "招标公告状态（招标阶段流程状态）1发布/2开标/3评标/4二次洽商/5定标报告/6中标公示/7结果发布")
    private Integer noticeStatus;

    @ApiModelProperty(value =  "招标公告状态文本")
    private String noticeStatusText;

    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

    @ApiModelProperty(value = "交易标的物")
    private Integer subjectMatterType;

    @ApiModelProperty(value =  "最小核算项目名称（多个项目以，隔开）")
    private String minProjectName;

}
