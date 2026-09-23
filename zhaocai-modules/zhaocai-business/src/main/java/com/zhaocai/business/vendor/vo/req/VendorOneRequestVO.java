package com.zhaocai.business.vendor.vo.req;

import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorContact;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 供应商注册请求
 *
 * @author chenming
 * @date 2024/05/29
 */
@Data
public class VendorOneRequestVO {

    @ApiModelProperty(value = "供应商基本信息")
    private Vendor vendor;

    @ApiModelProperty(value = "供应商联系人")
    private VendorContact vendorContact;

    @ApiModelProperty(value = "营业执照")
    private VendorCertificationRequestVO businessLicense;

    @ApiModelProperty(value = "诚信合规材料")
    private VendorCertificationRequestVO integrity;

    @ApiModelProperty(value = "法人授权书")
    private VendorCertificationRequestVO legalAuthorization;

    @ApiModelProperty(value = "相关资质")
    private List<VendorCertificationRequestVO> relevantCertificationList;

    @ApiModelProperty(value = "营业执照")
    private List<VendorCertificationRequestVO> businessLicenseList;

    @ApiModelProperty(value = "诚信合规材料")
    private List<VendorCertificationRequestVO> integrityList;

    @ApiModelProperty(value = "法人授权书")
    private List<VendorCertificationRequestVO> legalAuthorizationList;
}
