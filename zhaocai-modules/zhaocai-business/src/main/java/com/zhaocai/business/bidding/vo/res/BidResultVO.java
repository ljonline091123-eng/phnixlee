package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ssy
 * @date 2024/9/29 9:59
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BidResultVO", description = "中标结果数据列表VO")
public class BidResultVO {

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    @ApiModelProperty(value = "中标结果（0未中标 1已中标）")
    private Integer bidResult;

    @ApiModelProperty(value = "中标结果（0未中标 1已中标）")
    private String bidResultText;

}
