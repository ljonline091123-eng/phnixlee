package com.zhaocai.business.bidding.vo.req.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ssy
 * @date 2024/6/25 10:07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "VendorPortalNoticePageQueryVO", description = "招标公告列表数据-分页VO")
public class VendorPortalNoticePageQueryVO extends PageRecive implements Serializable {
    private static final long serialVersionUID = 73515314821312878L;

    @ApiModelProperty(value =  "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value =  "当前日期")
    private Date nowDate;

    @ApiModelProperty(value =  "招标公告状态")
    private Integer noticeStatus;

    @ApiModelProperty(value =  "采购计划类别")
    private Integer procurementPlanType;

    @ApiModelProperty(value =  "采购方案类型（1公开招标 2邀请招标 3询价采购 4单一来源）")
    private Integer schemeType;


}
