package com.zhaocai.business.vendor.controller;

import com.alibaba.fastjson.JSONObject;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.manager.http.dto.req.BpmInitializeRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmListProcessLogRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmLoadTaskDefRequestDTO;
import com.zhaocai.business.manager.http.dto.res.BpmInitializeResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmListProcessLogResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmLoadTaskDefResponseDTO;
import com.zhaocai.business.vendor.service.IVendorChangeService;
import com.zhaocai.business.vendor.vo.req.VendorChangeRequestVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

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



    @ApiOperation(value = "初始化接口")
    @GetMapping("/initialize")
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        return vendorChangeService.initialize(requestDTO);
    }

    @ApiOperation(value = "流程操作日志列表接口")
    @GetMapping ("/listProcessLog")
    public ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        return vendorChangeService.listProcessLog(requestDTO);
    }

    @ApiOperation(value = "审批")
    @PostMapping ("/audit")
    public ResultData<String> audit(@ApiIgnore @RequestBody JSONObject body) {
        String processKey = body.getString("processKey");
        body.remove("processKey");
        try {
            return ResultData.data(vendorChangeService.audit(processKey, body));
        } catch (Exception e) {
            return ResultData.fail(e.getLocalizedMessage());
        }
    }

    @ApiOperation(value = "加载定义接口")
    @GetMapping("/loadTaskDef")
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        return vendorChangeService.loadTaskDef(requestDTO);
    }

}
