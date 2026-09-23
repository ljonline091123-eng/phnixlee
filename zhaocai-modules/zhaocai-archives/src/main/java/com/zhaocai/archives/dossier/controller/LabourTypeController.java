package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.LabourType;
import com.zhaocai.archives.dossier.domain.MaterialDetails;
import com.zhaocai.archives.dossier.service.ILabourTypeService;
import com.zhaocai.archives.dossier.tree.LabourTypeTree;
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
import java.util.Map;

/**
 * 劳务分类Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/labourType")
public class LabourTypeController extends BaseController {
    @Autowired
    private ILabourTypeService labourTypeService;

    /**
     * 查询劳务分类列表
     */
    // @RequiresPermissions("archives:labourType:list")
    @GetMapping("/list")
    public TableDataInfo list(LabourType labourType) {
        startPage();
        List<LabourType> list = labourTypeService.selectLabourTypeList(labourType);
        return getDataTable(list);
    }


    /**
     * 获取材料分类树列表
     *
     * @param labourType
     * @return
     */
    @GetMapping("/getLabourTypeTree")
    public List<LabourTypeTree> getLabourTypeTree(LabourType labourType) {
        return labourTypeService.getLabourTypeTree(labourType);
    }


    /**
     * 初始化
     */
    // @RequiresPermissions("archives:materialType:list")
    @GetMapping("/initData")
    public AjaxResult initData(LabourType labourType) {
        if (StringUtils.isEmpty(labourType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        labourTypeService.initData(labourType);
        return toAjax(true);
    }


    /**
     * 新增材料类型生成编号
     */
    // @RequiresPermissions("archives:materialType:add")
    @GetMapping("/initLabourType")
    public AjaxResult initLabourType(LabourType labourType) {
        return success(labourTypeService.initMaterialType(labourType));
    }


    /**
     * 导出劳务分类列表
     */
    // @RequiresPermissions("archives:labourType:export")
    @Log(title = "劳务分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LabourType labourType) {
        List<LabourType> list = labourTypeService.selectLabourTypeList(labourType);
        ExcelUtil<LabourType> util = new ExcelUtil<LabourType>(LabourType.class);
        util.exportExcel(response, list, "劳务分类数据");
    }

    /**
     * 获取劳务分类详细信息
     */
    // @RequiresPermissions("archives:labourType:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(labourTypeService.selectLabourTypeById(id));
    }

    /**
     * 新增劳务分类
     */
    // @RequiresPermissions("archives:labourType:add")
    @Log(title = "劳务分类", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LabourType labourType) {
        return toAjax(labourTypeService.insertLabourType(labourType));
    }

    /**
     * 修改劳务分类
     */
    // @RequiresPermissions("archives:labourType:edit")
    @Log(title = "劳务分类", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LabourType labourType) {
        return toAjax(labourTypeService.updateLabourType(labourType));
    }

    /**
     * 删除劳务分类
     */
    // @RequiresPermissions("archives:labourType:remove")
    @Log(title = "劳务分类", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(labourTypeService.deleteLabourTypeByIds(ids));
    }



    /**
     * 审核通过
     *
     * @param variables
     */
    @PostMapping("/processAuditPass")
    public AjaxResult processAuditPass(@RequestBody Map<String, Object> variables) {
        labourTypeService.processAuditPass(variables);
        return success();
    }
//
//    /**
//     * 审核通过
//     *
//     * @param variables
//     */
//    @PostMapping("/processAuditPass")
//    public void processAuditPass(@RequestBody Map<String, Object> variables) {
//        labourTypeService.processAuditPass(variables);
//    }



    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody LabourType materialType) {
        return toAjax(labourTypeService.addToMain(materialType));
    }

    /**
     * 关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/associationToMain")
    public AjaxResult associationToMain(@RequestBody LabourType materialType) {
        return toAjax(labourTypeService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody LabourType materialType) {
        return toAjax(labourTypeService.unAssociationToMain(materialType));
    }



}
