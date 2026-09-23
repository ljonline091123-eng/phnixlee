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
 * @date 2024/6/19 21:33
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingResultListVO", description = "定标供应商数据列表VO")
public class BiddingResultListVO implements Serializable {
    private static final long serialVersionUID = -1807818668569278950L;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "公示期时间-起")
    private Date publicityStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "公示期时间-止")
    private Date publicityEndTime;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    @ApiModelProperty(value = "中标候选人名次")
    private String candidate;

    /**
     * 中标结果（0未中标 1已中标）
     */
    @ApiModelProperty(value = "中标结果（0未中标 1已中标）")
    private Integer bidResult;

}
