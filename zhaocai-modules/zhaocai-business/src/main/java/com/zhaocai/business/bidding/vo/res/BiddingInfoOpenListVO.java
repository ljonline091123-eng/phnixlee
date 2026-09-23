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
 * @date 2024/6/17 16:50
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingInfoOpenListVO", description = "投标单开标信息列表VO")
public class BiddingInfoOpenListVO implements Serializable {

    private static final long serialVersionUID = 8886973597609298897L;

    @ApiModelProperty(value =  "投标单id")
    private Long id;

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    @ApiModelProperty(value =  "联系人")
    private String contact;

    @ApiModelProperty(value =  "联系电话")
    private String phone;

    @ApiModelProperty(value =  "是否收取保证金")
    private Integer collectDeposit;

    @ApiModelProperty(value =  "投标状态")
    private Integer biddingStatus;

    @ApiModelProperty(value =  "投标状态（文本）")
    private String biddingStatusText;

    @ApiModelProperty(value = "创建时间（投标时间）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;

    @ApiModelProperty(value =  "操作人ip")
    private String ipAddress;

    @ApiModelProperty(value =  "是否开标（0待开标 1已开标）")
    private String biddingOpenStatus;

    @ApiModelProperty(value =  "是否设置开标人员")
    private String openPeopleStatus;

}
