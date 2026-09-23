package com.zhaocai.business.vendor.controller;

import com.alibaba.fastjson.JSONObject;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.manager.http.dto.req.BpmInitializeRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmListProcessLogRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmLoadTaskDefRequestDTO;
import com.zhaocai.business.manager.http.dto.res.BpmInitializeResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmListProcessLogResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmLoadTaskDefResponseDTO;
import com.zhaocai.business.vendor.service.IVendorChangeLevelService;
import com.zhaocai.business.vendor.vo.req.VendorLevelRequestVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

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



    @ApiOperation(value = "初始化接口")
    @GetMapping("/initialize")
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        return vendorChangeLevelService.initialize(requestDTO);
    }

    @ApiOperation(value = "流程操作日志列表接口")
    @GetMapping ("/listProcessLog")
    public ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        return vendorChangeLevelService.listProcessLog(requestDTO);
    }

    @ApiOperation(value = "审批")
    @PostMapping ("/audit")
    public ResultData<String> audit(@ApiIgnore @RequestBody JSONObject body) {
        String processKey = body.getString("processKey");
        body.remove("processKey");
        try {
            return ResultData.data(vendorChangeLevelService.audit(processKey, body));
        } catch (Exception e) {
            return ResultData.fail(e.getLocalizedMessage());
        }
    }

    @ApiOperation(value = "加载定义接口")
    @GetMapping("/loadTaskDef")
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        return vendorChangeLevelService.loadTaskDef(requestDTO);
    }

}
