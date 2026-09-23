package com.zhaocai.business.vendor.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;


/**
 * 供应商资质
 *
 * @author chenming
 * @date 2024/05/30
 */
@Data
public class VendorCertificationListVO {

    @ApiModelProperty(value = "营业执照")
    private VendorCertificationVO businessLicense;

    @ApiModelProperty(value = "诚信合规材料")
    private VendorCertificationVO integrity;

    @ApiModelProperty(value = "法人授权书")
    private List<VendorCertificationVO> legalAuthorizationList;

    @ApiModelProperty(value = "相关资质")
    private List<VendorCertificationVO> relevantCertificationList;

    @ApiModelProperty(value = "营业执照")
    private List<VendorCertificationVO> businessLicenseList;

    @ApiModelProperty(value = "诚信合规材料")
    private List<VendorCertificationVO> integrityList;
}
