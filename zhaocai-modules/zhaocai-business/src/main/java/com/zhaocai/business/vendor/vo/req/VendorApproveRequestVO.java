package com.zhaocai.business.vendor.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 供应商审核请求vo
 *
 * @author chenming
 * @date 2024/05/31
 */
@Data
public class VendorApproveRequestVO {
    @ApiModelProperty(value =  "id")
    private Long id;

    @ApiModelProperty(value =  "供应商分类")
    private Integer vendorClass;

    @ApiModelProperty(value =  "供应商等级")
    private Integer vendorLevel;
}
