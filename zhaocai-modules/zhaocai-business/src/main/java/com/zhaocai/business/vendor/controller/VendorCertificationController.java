package com.zhaocai.business.vendor.controller;

import com.zhaocai.business.common.annotations.VendorStateCheck;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.enums.CertificationTypeEnum;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.IVendorCertificationService;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.VendorCertificationRequestVO;
import com.zhaocai.business.vendor.vo.res.VendorCertificationListVO;
import com.zhaocai.business.vendor.vo.res.VendorMainContactVO;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import com.zhaocai.common.security.utils.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 企业资质
 *
 * @author chenming
 * @date 2024/05/30
 */
@Api("企业资质")
@RestController
@RequestMapping("/vendorCertification")
public class VendorCertificationController extends BladeController {

    @Autowired
    private IVendorCertificationService vendorCertificationService;

    @Autowired
    private IVendorService vendorService;

    @Autowired
    private IVendorContactService vendorContactService;

    /**
     * 获取企业资质列表
     */
    @VendorStateCheck
    @GetMapping("/listCertification")
    @ApiOperation(value = "获取企业资质列表")
    public ResultData<VendorCertificationListVO> listCertification() {
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());

        // 获取主要联系人
        VendorMainContactVO mainContact = vendorContactService.getMainContact(vendor.getId());

        return ResultData.data(vendorCertificationService.listCertification(vendor.getId(),mainContact.getId()));
    }

    /**
     * 删除企业资质列表
     */
    @VendorStateCheck
    @Log(title = "删除企业资质列表",businessType = BusinessType.DELETE)
    @PostMapping("/deleteCertification")
    @ApiOperation(value = "删除企业资质列表")
    public ResultData<Boolean> deleteCertification(@RequestParam Long id){
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        vendorCertificationService.deleteCertification(vendor.getId(),id);
        return ResultData.success();
    }

    /**
     * 新增企业资质-营业执照
     */
    @VendorStateCheck
    @Log(title = "新增企业资质-营业执照",businessType = BusinessType.INSERT)
    @PostMapping("/saveCertificationBusiness")
    @ApiOperation(value = "新增企业资质-营业执照")
    public ResultData<Boolean> saveCertificationBusiness(@RequestBody VendorCertificationRequestVO requestVO){
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        vendorCertificationService.addCertification(requestVO,vendor.getId(), CertificationTypeEnum.BUSINESS_LICENSE);

        return ResultData.success();
    }

    /**
     * 新增企业资质-诚信合规材料
     */
    @VendorStateCheck
    @Log(title = "新增企业资质-诚信合规材料",businessType = BusinessType.INSERT)
    @PostMapping("/saveCertificationIntegrity")
    @ApiOperation(value = "新增企业资质-诚信合规材料")
    public ResultData<Boolean> saveCertificationIntegrity(@RequestBody VendorCertificationRequestVO requestVO){
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        vendorCertificationService.addCertification(requestVO,vendor.getId(), CertificationTypeEnum.INTEGRITY);

        return ResultData.success();
    }

    /**
     * 新增企业资质-法人授权书
     */
    @VendorStateCheck
    @Log(title = "新增企业资质-法人授权书",businessType = BusinessType.INSERT)
    @PostMapping("/saveCertificationLegal")
    @ApiOperation(value = "新增企业资质-法人授权书")
    public ResultData<Boolean> saveCertificationLegal(@RequestBody VendorCertificationRequestVO requestVO){
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        vendorCertificationService.addCertification(requestVO,vendor.getId(), CertificationTypeEnum.LEGAL_AUTHORIZATION);

        return ResultData.success();
    }

    /**
     * 新增企业资质-相关资质
     */
    @VendorStateCheck
    @Log(title = "新增企业资质-相关资质",businessType = BusinessType.INSERT)
    @PostMapping("/saveCertificationRelevant")
    @ApiOperation(value = "新增企业资质-相关资质")
    public ResultData<Boolean> sveCertificationRelevant(@RequestBody VendorCertificationRequestVO requestVO){
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        vendorCertificationService.addCertification(requestVO,vendor.getId(), CertificationTypeEnum.RELEVANT_CERTIFICATION);

        return ResultData.success();
    }
}
