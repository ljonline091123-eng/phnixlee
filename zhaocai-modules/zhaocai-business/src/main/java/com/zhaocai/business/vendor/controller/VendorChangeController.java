package com.zhaocai.business.vendor.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.vendor.service.IVendorChangeService;
import com.zhaocai.business.vendor.vo.req.VendorChangeRequestVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 供应商变更Controller
 *
 * @author lsn
 * @date 2024-08-05
 */
@Api("供应商变更")
@RestController
@RequestMapping("/vendorChange")
public class VendorChangeController extends BladeController {

    @Autowired
    private IVendorChangeService vendorChangeService;

    /**
     * 供应商修改详情
     *
     * @param vendorId 供应商id
     * @return
     */
    @GetMapping("/updateDetail")
    @ApiOperation("供应商修改详情")
    public ResultData<VendorChangeRequestVO> updateDetail(Long vendorId) {
        return ResultData.data(vendorChangeService.getVendorUpdateDetail(vendorId));
    }

    /**
     * 保存供应商变更信息
     */
    @PostMapping("/saveVendorChance")
    @ApiOperation("保存供应商变更信息")
    public ResultData<Boolean> saveVendorChance(@RequestBody @Valid VendorChangeRequestVO requestVO) {
        vendorChangeService.saveVendorChange(requestVO);
        return ResultData.success();
    }

    /**
     * 提交供应商变更信息
     */
    @PostMapping("/submitVendorChance")
    @ApiOperation("提交供应商变更信息")
    public ResultData<Boolean> submitVendorChance(@RequestBody @Valid VendorChangeRequestVO requestVO) {
        vendorChangeService.submitVendorChance(requestVO);
        return ResultData.success();
    }

}
