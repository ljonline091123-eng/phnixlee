package com.zhaocai.business.vendor.vo.req;

import com.zhaocai.business.vendor.domain.VendorCertificationChange;
import com.zhaocai.business.vendor.domain.VendorChange;
import com.zhaocai.business.vendor.domain.VendorContactChange;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 供应商变更保存请求
 *
 * @author lsn
 * @date 2024/08/05
 */
@Data
public class VendorChangeRequestVO {

    @ApiModelProperty(value = "供应商基本信息变更")
    private VendorChange vendorChange;

    @ApiModelProperty(value = "供应商主要联系人变更")
    private VendorContactChange mainContactChange;

    @ApiModelProperty(value = "供应商联系人变更")
    private List<VendorContactChange> contactChangeList;

    @ApiModelProperty(value = "供应商资质变更信息")
    private List<VendorCertificationChange> certificationChangeList;

    @ApiModelProperty(value = "营业执照")
    private VendorCertificationChange businessLicense;

    @ApiModelProperty(value = "诚信合规材料")
    private VendorCertificationChange integrity;

    @ApiModelProperty(value = "营业执照")
    private List<VendorCertificationChange> businessLicenseList;

    @ApiModelProperty(value = "诚信合规材料")
    private List<VendorCertificationChange> integrityList;

    @ApiModelProperty(value = "法人授权书")
    private List<VendorCertificationChange> legalAuthorizationList;

    @ApiModelProperty(value = "相关资质")
    private List<VendorCertificationChange> relevantCertificationList;

}
