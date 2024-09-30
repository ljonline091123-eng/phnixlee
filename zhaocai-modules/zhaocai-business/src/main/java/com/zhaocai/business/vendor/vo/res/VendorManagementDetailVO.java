package com.zhaocai.business.vendor.vo.res;

import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.vendor.vo.req.VendorBlackRequestVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 供应商管理详情
 *
 * @author chenming
 * @date 2024/05/31
 */
@Data
@Builder
public class VendorManagementDetailVO extends AdviceObject {

    @ApiModelProperty(value = "供应商基本信息")
    private VendorVO vendor;

    @ApiModelProperty(value = "供应商主要联系人")
    private VendorMainContactVO mainContact;

    @ApiModelProperty(value = "供应商资质")
    private VendorCertificationListVO certificationList;

    @ApiModelProperty(value = "供应商状态")
    private VendorStateVO vendorState;

    @ApiModelProperty(value = "供应商状态黑名单信息")
    private VendorBlackRequestVO vendorBlack;
}
