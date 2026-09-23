package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ssy
 * @date 2024/6/3 16:27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingOpenPeopleListVO", description = "开标人员信息列表VO")
public class BiddingOpenPeopleListVO implements Serializable {
    private static final long serialVersionUID = -5763668097173820839L;

    /** 招标公告id */
    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    /** 采购方案id */
    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    /** 人员id */
    @ApiModelProperty(value =  "人员id")
    private Long userId;

    /** 人员姓名 */
    @ApiModelProperty(value =  "人员姓名")
    private String userName;

    /** 是否开标（0未开标 1已开标） */
    @ApiModelProperty(value =  "是否开标（0未开标 1已开标）")
    private Integer isOpen;

    /** 创建时间（开标时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间（开标时间）")
    private Date createTime;

    /** 是否开标（文本） */
    @ApiModelProperty(value =  "是否开标（文本）")
    private String isOpenText;

    @ApiModelProperty(value = "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value = "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value = "采购经办人名称")
    private String procurementOfficerName;

    @ApiModelProperty(value =  "是否为开标人（0否 1是）")
    private Integer isOpenUser;

}
