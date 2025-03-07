package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.SubcontractingType;
import com.zhaocai.archives.dossier.service.ISubcontractingTypeService;
import com.zhaocai.archives.dossier.tree.SubcontractingTypeTree;
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
 * 专业分包分类Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/subcontractingType")
public class SubcontractingTypeController extends BaseController {
    @Autowired
    private ISubcontractingTypeService subcontractingTypeService;

    /**
     * 查询专业分包分类列表
     */
    // @RequiresPermissions("archives:subcontractingType:list")
    @GetMapping("/list")
    public TableDataInfo list(SubcontractingType subcontractingType) {
        startPage();
        List<SubcontractingType> list = subcontractingTypeService.selectSubcontractingTypeList(subcontractingType);
        return getDataTable(list);
    }

    /**
     * 获取分类树列表
     *
     * @param subcontractingType
     * @return
     */
    @GetMapping("/getSubcontractingTypeTree")
    public List<SubcontractingTypeTree> getSubcontractingTypeTree(SubcontractingType subcontractingType) {
        return subcontractingTypeService.getSubcontractingTypeTree(subcontractingType);
    }


    /**
     * 初始化
     */
    // @RequiresPermissions("archives:materialType:list")
    @GetMapping("/initData")
    public AjaxResult initData(SubcontractingType subcontractingType) {
        if (StringUtils.isEmpty(subcontractingType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        subcontractingTypeService.initData(subcontractingType);
        return toAjax(true);
    }


    /**
     * 新增类型生成编号
     */
    // @RequiresPermissions("archives:materialType:add")
    @GetMapping("/initSubcontractingType")
    public AjaxResult initSubcontractingType(SubcontractingType subcontractingType) {
        return success(subcontractingTypeService.initSubcontractingType(subcontractingType));
    }


    /**
     * 导出专业分包分类列表
     */
    // @RequiresPermissions("archives:subcontractingType:export")
    @Log(title = "专业分包分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SubcontractingType subcontractingType) {
        List<SubcontractingType> list = subcontractingTypeService.selectSubcontractingTypeList(subcontractingType);
        ExcelUtil<SubcontractingType> util = new ExcelUtil<SubcontractingType>(SubcontractingType.class);
        util.exportExcel(response, list, "专业分包分类数据");
    }

    /**
     * 获取专业分包分类详细信息
     */
    // @RequiresPermissions("archives:subcontractingType:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(subcontractingTypeService.selectSubcontractingTypeById(id));
    }

    /**
     * 新增专业分包分类
     */
    // @RequiresPermissions("archives:subcontractingType:add")
    @Log(title = "专业分包分类", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody SubcontractingType subcontractingType) {
        return toAjax(subcontractingTypeService.insertSubcontractingType(subcontractingType));
    }

    /**
     * 修改专业分包分类
     */
    // @RequiresPermissions("archives:subcontractingType:edit")
    @Log(title = "专业分包分类", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody SubcontractingType subcontractingType) {
        return toAjax(subcontractingTypeService.updateSubcontractingType(subcontractingType));
    }

    /**
     * 删除专业分包分类
     */
    // @RequiresPermissions("archives:subcontractingType:remove")
    @Log(title = "专业分包分类", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(subcontractingTypeService.deleteSubcontractingTypeByIds(ids));
    }

    /**
     * 审核通过
     *
     * @param variables
     */
    @PostMapping("/processAuditPass")
    public AjaxResult processAuditPass(@RequestBody Map<String, Object> variables) {
        subcontractingTypeService.processAuditPass(variables);
        return toAjax(true);
    }



    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody SubcontractingType materialType) {
        return toAjax(subcontractingTypeService.addToMain(materialType));
    }

    /**
     * 关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/associationToMain")
    public AjaxResult associationToMain(@RequestBody SubcontractingType materialType) {
        return toAjax(subcontractingTypeService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody SubcontractingType materialType) {
        return toAjax(subcontractingTypeService.unAssociationToMain(materialType));
    }


}
