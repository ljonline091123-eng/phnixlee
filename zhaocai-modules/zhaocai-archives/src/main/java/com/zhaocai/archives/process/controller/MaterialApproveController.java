package com.zhaocai.archives.process.controller;

import com.alibaba.fastjson2.JSONObject;
import com.zhaocai.archives.common.vo.req.BpmInitializeRequestDTO;
import com.zhaocai.archives.common.vo.res.BpmInitializeResponseDTO;
import com.zhaocai.archives.process.domain.MaterialApprove;
import com.zhaocai.archives.process.service.IMaterialApproveService;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.web.page.TableDataInfo;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 物料审批辅Controller
 *
 * @author lzq
 * @date 2025-02-12
 */
@RestController
@RequestMapping("/materialApprove")
public class MaterialApproveController extends BaseController {
    @Autowired
    private IMaterialApproveService materialApproveService;

    /**
     * 查询物料审批辅列表
     */
    //@RequiresPermissions("process:approve:list")
    @GetMapping("/list")
    public TableDataInfo list(MaterialApprove materialApprove) {
        startPage();
        List<MaterialApprove> list = materialApproveService.selectMaterialApproveList(materialApprove);
        return getDataTable(list);
    }

    /**
     * 导出物料审批辅列表
     */
    //@RequiresPermissions("process:approve:export")
    @Log(title = "物料审批辅", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MaterialApprove materialApprove) {
        List<MaterialApprove> list = materialApproveService.selectMaterialApproveList(materialApprove);
        ExcelUtil<MaterialApprove> util = new ExcelUtil<MaterialApprove>(MaterialApprove.class);
        util.exportExcel(response, list, "物料审批辅数据");
    }

    /**
     * 获取物料审批辅详细信息
     */
    //@RequiresPermissions("process:approve:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(materialApproveService.selectMaterialApproveById(id));
    }

    /**
     * 新增物料审批辅
     */
    //@RequiresPermissions("process:approve:add")
    @Log(title = "物料审批辅", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MaterialApprove materialApprove) {
        return toAjax(materialApproveService.insertMaterialApprove(materialApprove));
    }

    /**
     * 修改物料审批辅
     */
    //@RequiresPermissions("process:approve:edit")
    @Log(title = "物料审批辅", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MaterialApprove materialApprove) {
        return toAjax(materialApproveService.updateMaterialApprove(materialApprove));
    }

    /**
     * 删除物料审批辅
     */
    //@RequiresPermissions("process:approve:remove")
    @Log(title = "物料审批辅", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(materialApproveService.deleteMaterialApproveByIds(ids));
    }


    /**
     * 提交
     */
    @Log(title = "提交材料档案", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    @ApiOperation("提交材料档案")
    public ResultData submit(@RequestBody MaterialApprove materialApprove) {
        return ResultData.status(materialApproveService.submit(materialApprove));
    }


    /**
     * 提交后处理
     *
     * @param variables
     */
    @PostMapping("/processStart")
    public void processStart(@RequestBody Map<String, Object> variables) {
        materialApproveService.processStart(variables);
    }

    /**
     * 审核通过
     *
     * @param variables
     */
    @PostMapping("/processAuditPass")
    public void processAuditPass(@RequestBody Map<String, Object> variables) {
        materialApproveService.processAuditPass(variables);
    }


//    /**
//     * 审核通过
//     *
//     * @param variables
//     */
//    @PostMapping("/processAuditPass")
//    public AjaxResult processAuditPass(@RequestBody Map<String, Object> variables) {
//        materialApproveService.processAuditPass(variables);
//        return AjaxResult.success();
//    }


    /**
     * 审批驳回到发起人
     *
     * @param variables
     */
    @PostMapping("/processAuditFreedom")
    public void processAuditFreedom(@RequestBody Map<String, Object> variables) {
        materialApproveService.processAuditFreedom(variables);
    }


    /**
     * 审批驳回到发起人
     *
     * @param variables
     */
    @PostMapping("/processAuditReject")
    public void processAuditReject(@RequestBody Map<String, Object> variables) {
        materialApproveService.processAuditReject(variables);
    }

    /**
     * 审批驳回到发起人
     *
     * @param variables
     */
    @PostMapping("/processAuditRevoke")
    public void processAuditRevoke(@RequestBody Map<String, Object> variables) {
        materialApproveService.processAuditRevoke(variables);
    }

    @ApiOperation(value = "审批")
    @PostMapping("/audit")
    public ResultData<String> audit(@ApiIgnore @RequestBody JSONObject body) {
        String processKey = body.getString("processKey");
        body.remove("processKey");
        try {
            String audit = materialApproveService.audit(processKey, body);
            return ResultData.data(audit);
        } catch (Exception e) {
            return ResultData.fail(e.getLocalizedMessage());
        }
    }

    @ApiOperation(value = "流程初始化接口")
    @GetMapping("/initialize")
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        try {
            return ResultData.data(materialApproveService.initialize(requestDTO));
        } catch (Exception e) {
            return ResultData.fail(e.getLocalizedMessage());
        }
    }

    @ApiOperation(value = "流程撤回接口")
    @GetMapping("/revokeProcess")
    public ResultData<Boolean> revokeProcess(Long id) {
        ResultData<String> stringResultData = materialApproveService.revokeProcess(id);
        if (stringResultData.getCode() == 200) {
            return ResultData.success();
        } else {
            return ResultData.fail(stringResultData.getMsg());
        }

    }


}
