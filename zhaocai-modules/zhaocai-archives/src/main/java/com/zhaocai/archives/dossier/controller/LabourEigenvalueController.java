package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.LabourEigenvalue;
import com.zhaocai.archives.dossier.service.ILabourEigenvalueService;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.web.page.TableDataInfo;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 劳务特征值Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/labourEigenvalue")
public class LabourEigenvalueController extends BaseController {
    @Autowired
    private ILabourEigenvalueService labourEigenvalueService;

    /**
     * 查询劳务特征值列表
     */
    // @RequiresPermissions("archives:labourEigenvalue:list")
    @GetMapping("/list")
    public TableDataInfo list(LabourEigenvalue labourEigenvalue) {
//        startPage();
        List<LabourEigenvalue> list = labourEigenvalueService.selectLabourEigenvalueList(labourEigenvalue);
        return getDataTable(list);
    }

    /**
     * 初始化材料特征项列表
     */
    // @RequiresPermissions("archives:materialItem:list")
    @GetMapping("/initData")
    public AjaxResult initData(LabourEigenvalue labourEigenvalue) {
        if (StringUtils.isEmpty(labourEigenvalue.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        labourEigenvalueService.initData(labourEigenvalue);
        return toAjax(true);
    }


    /**
     * 导出劳务特征值列表
     */
    // @RequiresPermissions("archives:labourEigenvalue:export")
    @Log(title = "劳务特征值", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LabourEigenvalue labourEigenvalue) {
        List<LabourEigenvalue> list = labourEigenvalueService.selectLabourEigenvalueList(labourEigenvalue);
        ExcelUtil<LabourEigenvalue> util = new ExcelUtil<LabourEigenvalue>(LabourEigenvalue.class);
        util.exportExcel(response, list, "劳务特征值数据");
    }

    /**
     * 获取劳务特征值详细信息
     */
    // @RequiresPermissions("archives:labourEigenvalue:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(labourEigenvalueService.selectLabourEigenvalueById(id));
    }

    /**
     * 新增劳务特征值
     */
    // @RequiresPermissions("archives:labourEigenvalue:add")
    @Log(title = "劳务特征值", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LabourEigenvalue labourEigenvalue) {
        return toAjax(labourEigenvalueService.insertLabourEigenvalue(labourEigenvalue));
    }

    /**
     * 修改劳务特征值
     */
    // @RequiresPermissions("archives:labourEigenvalue:edit")
    @Log(title = "劳务特征值", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LabourEigenvalue labourEigenvalue) {
        return toAjax(labourEigenvalueService.updateLabourEigenvalue(labourEigenvalue));
    }

    /**
     * 删除劳务特征值
     */
    // @RequiresPermissions("archives:labourEigenvalue:remove")
    @Log(title = "劳务特征值", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(labourEigenvalueService.deleteLabourEigenvalueByIds(ids));
    }

    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody LabourEigenvalue materialType) {
        int i = labourEigenvalueService.addToMain(materialType);
        if(i == -1){
            return  AjaxResult.success("编码已存在");
        }
        return toAjax(i);
    }

    /**
     * 关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/associationToMain")
    public AjaxResult associationToMain(@RequestBody LabourEigenvalue materialType) {
        return toAjax(labourEigenvalueService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody LabourEigenvalue materialType) {
        return toAjax(labourEigenvalueService.unAssociationToMain(materialType));
    }

}
