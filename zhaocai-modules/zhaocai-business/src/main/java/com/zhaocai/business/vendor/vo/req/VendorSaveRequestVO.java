package com.zhaocai.business.vendor.vo.req;

import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorContact;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 供应商保存请求
 *
 * @author chenming
 * @date 2024/05/29
 */
@Data
public class VendorSaveRequestVO {

    @NotNull(message = "供应商基本信息不能为空")
    @ApiModelProperty(value = "供应商基本信息")
    private Vendor vendor;

    @NotNull(message = "供应商联系人不能为空")
    @ApiModelProperty(value = "供应商联系人")
    private VendorContact vendorContact;
}
