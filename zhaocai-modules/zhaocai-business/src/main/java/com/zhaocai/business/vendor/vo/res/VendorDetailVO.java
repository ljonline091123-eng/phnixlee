package com.zhaocai.business.vendor.vo.res;

import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 供应商详细信息
 *
 * @author chenming
 * @date 2024/05/30
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class VendorDetailVO extends AdviceObject {

    @ApiModelProperty(value = "供应商基本信息")
    private VendorVO vendor;

    @ApiModelProperty(value = "供应商主要联系人")
    private VendorMainContactVO mainContact;

    @ApiModelProperty(value = "供应商联系人")
    private List<VendorContactVO> contactList;
}
