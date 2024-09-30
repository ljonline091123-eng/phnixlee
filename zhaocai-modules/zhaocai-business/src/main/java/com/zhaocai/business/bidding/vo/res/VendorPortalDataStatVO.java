package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author ssy
 * @date 2024/6/25 15:09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "VendorPortalDataStatVO", description = "门户端数据统计VO")
public class VendorPortalDataStatVO {

    @ApiModelProperty(value =  "累计成交额")
    private BigDecimal transactionMoney;

    @ApiModelProperty(value =  "已入驻供应商数量")
    private Long vendorNum;

    @ApiModelProperty(value =  "项目数量")
    private Long projectNum;

}
