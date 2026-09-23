package com.zhaocai.business.vendor.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 供应商合作单位
 *
 * @author chenming
 * @date 2024-06-25
 */
@Data
public class VendorCooperativePartnerListQueryVO extends PageRecive {

    @ApiModelProperty(value = "供应商 id")
   private Long vendorId;
}
