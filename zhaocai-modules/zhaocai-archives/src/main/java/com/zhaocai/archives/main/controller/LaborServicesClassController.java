package com.zhaocai.archives.main.controller;

import com.zhaocai.archives.dossier.tree.LabourTypeTree;
import com.zhaocai.archives.main.domain.LaborServicesClass;
import com.zhaocai.archives.main.service.ILaborServicesClassService;
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
 * 劳务分类主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/laborClass")
public class LaborServicesClassController extends BaseController {
    @Autowired
    private ILaborServicesClassService laborServicesClassService;

    /**
     * 查询劳务分类主列表
     */
    // @RequiresPermissions("archives:laborClass:list")
    @GetMapping("/list")
    public TableDataInfo list(LaborServicesClass laborServicesClass) {
        startPage();
        List<LaborServicesClass> list = laborServicesClassService.selectLaborServicesClassList(laborServicesClass);
        return getDataTable(list);
    }

    /**
     * 导出劳务分类主列表
     */
    // @RequiresPermissions("archives:laborClass:export")
    @Log(title = "劳务分类主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LaborServicesClass laborServicesClass) {
        List<LaborServicesClass> list = laborServicesClassService.selectLaborServicesClassList(laborServicesClass);
        ExcelUtil<LaborServicesClass> util = new ExcelUtil<LaborServicesClass>(LaborServicesClass.class);
        util.exportExcel(response, list, "劳务分类主数据");
    }

    /**
     * 获取劳务分类主详细信息
     */
    // @RequiresPermissions("archives:laborClass:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(laborServicesClassService.selectLaborServicesClassById(id));
    }

    /**
     * 新增劳务分类主
     */
    // @RequiresPermissions("archives:laborClass:add")
    @Log(title = "劳务分类主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LaborServicesClass laborServicesClass) {
        return toAjax(laborServicesClassService.insertLaborServicesClass(laborServicesClass));
    }

    /**
     * 修改劳务分类主
     */
    // @RequiresPermissions("archives:laborClass:edit")
    @Log(title = "劳务分类主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LaborServicesClass laborServicesClass) {
        return toAjax(laborServicesClassService.updateLaborServicesClass(laborServicesClass));
    }

    /**
     * 删除劳务分类主
     */
    // @RequiresPermissions("archives:laborClass:remove")
    @Log(title = "劳务分类主", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(laborServicesClassService.deleteLaborServicesClassByIds(ids));
    }

    /**
     * 树
     *
     * @return
     */
    @GetMapping("/getLaborServicesClassTree")
    public List<LabourTypeTree> getLaborServicesClassTree() {
        return laborServicesClassService.getLaborServicesClassTree();
    }

    @GetMapping(value = "/initCode")
    public AjaxResult initCode(LaborServicesClass aClass) {
        return success(laborServicesClassService.initCode(aClass));
    }


}
