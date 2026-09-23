package com.zhaocai.business.vendor.controller;

import com.zhaocai.business.common.annotations.VendorStateCheck;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.enums.CertificationTypeEnum;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorCertificationChange;
import com.zhaocai.business.vendor.service.IVendorCertificationChangeService;
import com.zhaocai.business.vendor.service.IVendorChangeService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import com.zhaocai.common.security.utils.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 企业资质变更
 *
 * @author lsn
 * @date 2024/08/06
 */
@Api("企业资质变更")
@RestController
@RequestMapping("/vendorCertificationChange")
public class VendorCertificationChangeController extends BladeController {

    @Autowired
    private IVendorCertificationChangeService certificationChangeService;

    @Autowired
    private IVendorService vendorService;

    @Autowired
    private IVendorChangeService vendorChangeService;

    /**
     * 删除企业资质变更
     */
    @Log(title = "删除企业资质变更", businessType = BusinessType.DELETE)
    @PostMapping("/deleteCertification")
    @ApiOperation(value = "删除企业资质变更")
    public ResultData<Boolean> deleteCertification(@RequestParam Long id) {
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        certificationChangeService.deleteCertification(vendor.getId(), id);
        return ResultData.success();
    }

    /**
     * 新增企业资质变更-营业执照
     */
    @Log(title = "新增企业资质变更-营业执照", businessType = BusinessType.INSERT)
    @PostMapping("/saveCertificationBusiness")
    @ApiOperation(value = "新增企业资质变更-营业执照")
    public ResultData<Boolean> saveCertificationBusiness(@RequestBody VendorCertificationChange requestVO) {
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        Integer version = vendorChangeService.getLastVersion(vendor.getId());
        certificationChangeService.addCertification(requestVO, vendor.getId(), version, CertificationTypeEnum.BUSINESS_LICENSE);

        return ResultData.success();
    }

    /**
     * 新增企业资质变更-诚信合规材料
     */
    @VendorStateCheck
    @Log(title = "新增企业资质变更-诚信合规材料", businessType = BusinessType.INSERT)
    @PostMapping("/saveCertificationIntegrity")
    @ApiOperation(value = "新增企业资质变更-诚信合规材料")
    public ResultData<Boolean> saveCertificationIntegrity(@RequestBody VendorCertificationChange requestVO) {
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        Integer version = vendorChangeService.getLastVersion(vendor.getId());
        certificationChangeService.addCertification(requestVO, vendor.getId(), version, CertificationTypeEnum.INTEGRITY);

        return ResultData.success();
    }

    /**
     * 新增企业资质变更-法人授权书
     */
    @VendorStateCheck
    @Log(title = "新增企业资质变更-法人授权书", businessType = BusinessType.INSERT)
    @PostMapping("/saveCertificationLegal")
    @ApiOperation(value = "新增企业资质变更-法人授权书")
    public ResultData<Boolean> saveCertificationLegal(@RequestBody VendorCertificationChange requestVO) {
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        Integer version = vendorChangeService.getLastVersion(vendor.getId());
        certificationChangeService.addCertification(requestVO, vendor.getId(), version, CertificationTypeEnum.LEGAL_AUTHORIZATION);

        return ResultData.success();
    }

    /**
     * 新增企业资质-相关资质
     */
    @VendorStateCheck
    @Log(title = "新增企业资质变更-相关资质", businessType = BusinessType.INSERT)
    @PostMapping("/saveCertificationRelevant")
    @ApiOperation(value = "新增企业资质变更-相关资质")
    public ResultData<Boolean> sveCertificationRelevant(@RequestBody VendorCertificationChange requestVO) {
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        Integer version = vendorChangeService.getLastVersion(vendor.getId());
        certificationChangeService.addCertification(requestVO, vendor.getId(), version, CertificationTypeEnum.RELEVANT_CERTIFICATION);

        return ResultData.success();
    }

}
