package com.zhaocai.business.vendor.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.VendorBlackRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorLevelRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorManagementListQueryVO;
import com.zhaocai.business.vendor.vo.res.VendorManagementDetailVO;
import com.zhaocai.business.vendor.vo.res.VendorManagementListVO;
import com.zhaocai.business.vendor.vo.res.VendorPerformanceListVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 供应商管理
 *
 * @author chenming
 * @date 2024/05/31
 */
@Api("供应商管理")
@RestController
@RequestMapping("/vendor/management")
public class VendorManagementController extends BladeController {

    @Autowired
    private IVendorService  vendorService;

    /**
     * 供应商管理列表查询
     */
    @GetMapping("/listVendor")
    @ApiOperation(value = "供应商管理列表查询")
    public ResultData<PageResult<VendorManagementListVO>> listVendor(VendorManagementListQueryVO queryVO) {
        return ResultData.data(vendorService.listVendor(queryVO));
    }

    /**
     * 供应商管理详情
     */
    @GetMapping("/vendorDetail")
    @ApiOperation(value = "供应商管理详情")
    public ResultData<VendorManagementDetailVO> vendorDetail(@RequestParam Long id) {
        return ResultData.data(vendorService.getVendorManagementDetail(id));
    }

    /**
     * 修改供应商等级
     */
    @Log(title = "修改供应商等级",businessType = BusinessType.UPDATE)
    @PostMapping("/updateVendorLevel")
    @ApiOperation(value = "修改供应商等级")
    public ResultData<Boolean> updateVendorLevel(@RequestBody VendorLevelRequestVO requestVO) {
        vendorService.updateVendorLevel(requestVO);
        return ResultData.success();
    }

    /**
     * 修改供应商黑名单状态
     */
    @Log(title = "修改供应商黑名单状态",businessType = BusinessType.UPDATE)
    @PostMapping("/updateBlackState")
    @ApiOperation(value = "修改供应商黑名单状态")
    public ResultData<Boolean> updateBlackState(@RequestBody VendorBlackRequestVO requestVO) {
        vendorService.updateBlackState(requestVO);
        return ResultData.success();
    }


    /**
     * 获取供应商履约评价
     * @param id
     * @return
     */
    @GetMapping("/listVendorPerformance")
    @ApiOperation(value = "获取供应商履约评价")
    public ResultData<List<VendorPerformanceListVO>> listVendorPerformance(@RequestParam Long id) {
        return ResultData.data(vendorService.listVendorPerformance(id));
    }
}
