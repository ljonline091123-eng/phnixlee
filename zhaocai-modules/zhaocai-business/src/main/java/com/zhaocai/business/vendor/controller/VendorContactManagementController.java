package com.zhaocai.business.vendor.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.vendor.domain.VendorContact;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.business.vendor.vo.req.UpdateContactManagerRequestVO;
import com.zhaocai.business.vendor.vo.req.UpdateContactStateRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorContactListQueryVO;
import com.zhaocai.business.vendor.vo.res.VendorContactListVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 供应商联系人管理控
 *
 * @author chenming
 * @date 2024/06/03
 */
@RestController
@RequestMapping("/vendorContact/management")
@Api(value = "供应商联系人管理")
public class VendorContactManagementController extends BladeController {

    @Autowired
    private IVendorContactService vendorContactService;

    /**
     * 分页查询
     */
    @GetMapping("/listPage")
    @ApiOperation(value = "分页查询")
    public ResultData<PageResult<VendorContactListVO>> listPage(VendorContactListQueryVO queryVO) {
        PageResult<VendorContactListVO> pageResult = vendorContactService.listContactManagementList(queryVO);
        return ResultData.data(pageResult);
    }

    /**
     * 获取供应商联系授权书
     */
    @GetMapping("/getAuthorization")
    @ApiOperation(value = "获取供应商联系人授权书")
    public ResultData<AttachmentVO> getAuthorization(@RequestParam Long id) {
        return ResultData.data(vendorContactService.getAuthorization(id));
    }

    /**
     * 获取供应商联系人
     */
    @GetMapping("/getInfo")
    @ApiOperation(value = "根据联系人对象id获取供应商联系人详情")
    public ResultData<VendorContact> getInfo(@RequestParam Long id) {
        return ResultData.data(vendorContactService.getInfo(id));
    }

    /**
     * 更新供应商联系人状态
     */
    @PostMapping("/updateContactState")
    @ApiOperation(value = "更新供应商联系人状态")
    public ResultData<Boolean> updateContactState(@RequestBody UpdateContactStateRequestVO requestVO) {
        vendorContactService.updateContactState(requestVO.getContactId(),requestVO.getState());
        return ResultData.success();
    }

    /**
     * 设置供应商管理员
     */
    @PostMapping("/updateContactManager")
    @ApiOperation(value = "设置供应商管理员")
    public ResultData<Boolean> updateContactManager(@RequestBody UpdateContactManagerRequestVO requestVO) {
        vendorContactService.updateContactManager(requestVO);
        return ResultData.success();
    }
}
