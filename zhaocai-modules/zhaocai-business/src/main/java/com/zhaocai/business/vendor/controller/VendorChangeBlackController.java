package com.zhaocai.business.vendor.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.vendor.service.IVendorChangeBlackService;
import com.zhaocai.business.vendor.vo.req.VendorBlackRequestVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 供应商变更-黑名单Controller
 *
 * @author lsn
 * @date 2024-08-05
 */
@Api("供应商变更-黑名单")
@RestController
@RequestMapping("/vendorChangeBlack")
public class VendorChangeBlackController extends BladeController {

    @Autowired
    private IVendorChangeBlackService vendorChangeBlackService;

    /**
     * 保存供应商变更黑名单信息
     */
    @PostMapping("/saveVendorBlack")
    @ApiOperation(value = "保存供应商变更黑名单信息")
    public ResultData<Boolean> saveVendorBlack(@RequestBody VendorBlackRequestVO requestVO) {
        vendorChangeBlackService.saveVendorBlack(requestVO);
        return ResultData.success();
    }

}
