package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投标供应商
 *
 * @author chenming
 * @date 2024-06-18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BiddingVendorVO {

    @ApiModelProperty("供应商 id")
    private Long vendorId;

    @ApiModelProperty("供应商名称")
    private String vendorName;
}