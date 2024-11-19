package com.zhaocai.business.vendor.controller;

import cn.hutool.core.util.StrUtil;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.config.EnvironmentUtil;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.VendorRegisterRequestVO;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 易料供应商推送
 *
 * @author cs
 * @date 2024-10-22
 */

@RestController
@RequestMapping("/underling/vendorRegister")
public class UnderlingVendorRegisterController extends BladeController {

    @Autowired
    private IVendorService vendorService;

    /**
     * 易料供应商推送
     */
    @Log(title = "供应商推送",businessType = BusinessType.INSERT)
    @PostMapping("/register")
    @ApiOperation("供应商推送")
    public ResultData<Boolean> register(@RequestBody @Valid VendorRegisterRequestVO requestVO) {
       String firstCooperationCompanyCode =  EnvironmentUtil.getProperty("vendor.default.firstCooperationCompanyCode");
        String firstCooperationCompanyName =  EnvironmentUtil.getProperty("vendor.default.firstCooperationCompanyName");
        if (StrUtil.isBlank(requestVO.getVendor().getFirstCooperationCompanyCode())) {
            requestVO.getVendor().setFirstCooperationCompanyCode(firstCooperationCompanyCode);
            requestVO.getVendor().setFirstCooperationCompanyName(firstCooperationCompanyName);
        }
        //供应商保存(直接生效)
        vendorService.vendorRegister(requestVO);
        return ResultData.success();
    }
}
