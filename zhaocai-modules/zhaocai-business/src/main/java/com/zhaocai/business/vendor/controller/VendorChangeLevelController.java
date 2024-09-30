package com.zhaocai.business.vendor.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.vendor.service.IVendorChangeLevelService;
import com.zhaocai.business.vendor.vo.req.VendorLevelRequestVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 供应商变更-修改等级Controller
 *
 * @author lsn
 * @date 2024-08-15
 */
@Api("供应商变更-修改等级")
@RestController
@RequestMapping("/vendorChangeLevel")
public class VendorChangeLevelController extends BladeController {

    @Autowired
    private IVendorChangeLevelService vendorChangeLevelService;

    /**
     * 保存供应商变更等级信息
     */
    @PostMapping("/saveVendorLevel")
    @ApiOperation(value = "保存供应商变更等级信息")
    public ResultData<Boolean> saveVendorLevel(@RequestBody VendorLevelRequestVO requestVO) {
        vendorChangeLevelService.saveVendorLevel(requestVO);
        return ResultData.success();
    }

}
