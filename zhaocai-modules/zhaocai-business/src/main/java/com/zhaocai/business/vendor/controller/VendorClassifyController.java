package com.zhaocai.business.vendor.controller;

import com.zhaocai.business.vendor.service.IVendorClassifyService;
import com.zhaocai.business.vendor.vo.res.VendorClassifyTreeVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author ssy
 * @date 2024/7/13 16:08
 */
@Api("供应商分类")
@RestController
@RequestMapping("/vendorClassify")
public class VendorClassifyController {

    @Autowired
    private IVendorClassifyService vendorClassifyService;

    /**
     * 获取供应商分类树
     */
    @GetMapping("/getVendorClassifyTree")
    @ApiOperation("获取供应商分类树")
    public ResultData<List<VendorClassifyTreeVO>> getVendorClassifyTree() {
        return ResultData.data(vendorClassifyService.getVendorClassifyTree());
    }

}
