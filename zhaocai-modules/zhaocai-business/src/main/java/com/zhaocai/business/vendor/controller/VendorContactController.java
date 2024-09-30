package com.zhaocai.business.vendor.controller;

import com.zhaocai.business.common.annotations.VendorStateCheck;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.UpdateAuthorizationDateRequestVO;
import com.zhaocai.business.vendor.vo.req.UpdateAuthorizationFileRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorContactAddRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorContactSaveRequestVo;
import com.zhaocai.business.vendor.vo.res.VendorContactListVO;
import com.zhaocai.business.vendor.vo.res.VendorMainContactVO;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 供应商联系人Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@Api("供应商联系人")
@RestController
@RequestMapping("/vendorContact")
public class VendorContactController extends BladeController {

    @Autowired
    private IVendorContactService vendorContactService;

    @Autowired
    private IVendorService vendorService;

    /**
     * 获取供应商联系人
     */
    @VendorStateCheck
    @GetMapping("/listVendorContact")
    @ApiOperation(value = "获取供应商联系人")
    public ResultData<List<VendorContactListVO>> listVendorContact() {
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        return ResultData.data(vendorContactService.listVendorContact(vendor.getId()));
    }

    /**
     * 新增供应商联系人
     */
    @VendorStateCheck
    @PostMapping("/addVendorContact")
    @ApiOperation(value = "新增供应商联系人")
    public ResultData<Boolean> addVendorContact(@RequestBody VendorContactAddRequestVO requestVO) {
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        vendorContactService.addVendorContact(requestVO,vendor.getId());
        return ResultData.success();
    }

    /**
     * 修改供应商联系人
     */
    @VendorStateCheck
    @PostMapping("/updateVendorContact")
    @ApiOperation(value = "修改供应商联系人")
    public ResultData<Boolean> updateVendorContact(@RequestBody VendorContactSaveRequestVo requestVO) {
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        vendorContactService.updateVendorContact(requestVO,vendor.getId());
        return ResultData.success();
    }

    /**
     * 获取供应商联系授权书
     */
    @VendorStateCheck
    @GetMapping("/getAuthorization")
    @ApiOperation(value = "获取供应商联系人授权书")
    public ResultData<AttachmentVO> getAuthorization(@RequestParam Long id) {
        return ResultData.data(vendorContactService.getAuthorization(id));
    }

    /**
     * 修改供应商联系人状态
     */
    @VendorStateCheck
    @PostMapping("/updateContactState")
    @ApiOperation(value = "修改供应商联系人状态")
    public ResultData<Boolean> updateContactState(@RequestParam Long id,@RequestParam Integer state) {
        vendorContactService.updateContactState(id,state);
        return ResultData.success();
    }

    /**
     * 获取供应商主要联系人
     */
    @VendorStateCheck
    @ApiOperation(value = "获取供应商主要联系人")
    @GetMapping("/getVendorMainContact")
    public ResultData<VendorMainContactVO> getVendorMainContact() {
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        return ResultData.data(vendorContactService.getMainContact(vendor.getId()));
    }

    /**
     * 修改联系人授权书
     */
    @VendorStateCheck
    @PostMapping("/updateAuthorizationFile")
    @ApiOperation(value = "修改联系人授权书")
    public ResultData<Boolean> updateAuthorizationFile(@RequestBody @Valid UpdateAuthorizationFileRequestVO requestVO) {
        vendorContactService.updateAuthorizationFile(requestVO);
        return ResultData.success();
    }

    /**
     * 修改联系人授权书有效时间
     */
    @VendorStateCheck
    @PostMapping("/updateAuthorizationDate")
    @ApiOperation(value = "修改联系人授权书有效时间")
    public ResultData<Boolean> updateAuthorizationDate(@RequestBody UpdateAuthorizationDateRequestVO requestVO) {
        vendorContactService.updateAuthorizationDate(requestVO);
        return ResultData.success();
    }

    /**
     * 获取供应商联系人详情
     */
    @VendorStateCheck
    @ApiOperation(value = "获取供应商联系人详情")
    @GetMapping("/getVendorContactDetail")
    public ResultData<VendorContactSaveRequestVo> getVendorContactDetail(Long id) {
        return ResultData.data(vendorContactService.getVendorContactDetail(id));
    }

    /**
     * 企业联系人认证
     * @return
     */
    @PostMapping("/vendorContactSignAuth")
    @ApiOperation(value = "企业联系人认证")
    public ResultData<String> vendorContactSignAuth() {
        return ResultData.data(vendorContactService.vendorContactSignAuth());
    }
}
