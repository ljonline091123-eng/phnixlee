package com.zhaocai.business.vendor.controller;

import cn.hutool.core.util.StrUtil;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.config.EnvironmentUtil;
import com.zhaocai.business.common.enums.SupplierRegistSourceEnum;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.VendorManagementListQueryVO;
import com.zhaocai.business.vendor.vo.req.VendorRegisterRequestVO;
import com.zhaocai.business.vendor.vo.res.VendorVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/underling/vendor")
public class UnderlingVendorController extends BladeController {

    @Autowired
    private IVendorService vendorService;

    /**
     * 获取供应商列表
     */
    @GetMapping("/listVendor")
    public PageResult<VendorVO> listVendor(VendorManagementListQueryVO queryVO) {
        return vendorService.listVendorJkptht(queryVO);
    }

    /**
     * 第三方供应商注册
     */
    @Log(title = "供应商注册",businessType = BusinessType.INSERT)
    @PostMapping("/register")
    @ApiOperation("供应商注册")
    public ResultData<Boolean> register(@RequestBody @Valid VendorRegisterRequestVO requestVO) {
       String firstCooperationCompanyCode =  EnvironmentUtil.getProperty("vendor.default.firstCooperationCompanyCode");
        String firstCooperationCompanyName =  EnvironmentUtil.getProperty("vendor.default.firstCooperationCompanyName");
        if (StrUtil.isBlank(requestVO.getVendor().getFirstCooperationCompanyCode())) {
            requestVO.getVendor().setFirstCooperationCompanyCode(firstCooperationCompanyCode);
            requestVO.getVendor().setFirstCooperationCompanyName(firstCooperationCompanyName);
        }
        //供应商注册
        vendorService.register(requestVO);
        return ResultData.success();
    }
}
