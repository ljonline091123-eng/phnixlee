package com.zhaocai.archives.main.controller;

import com.zhaocai.archives.dossier.tree.MaterialTypeTree;
import com.zhaocai.archives.main.domain.MtrClass;
import com.zhaocai.archives.main.service.IMtrClassService;
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
 * 材料分类主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/mtrClass")
public class MtrClassController extends BaseController {
    @Autowired
    private IMtrClassService mtrClassService;

    /**
     * 查询材料分类主列表
     */
    // @RequiresPermissions("archives:mtrClass:list")
    @GetMapping("/list")
    public TableDataInfo list(MtrClass mtrClass) {
        startPage();
        List<MtrClass> list = mtrClassService.selectMtrClassList(mtrClass);
        return getDataTable(list);
    }

    /**
     * 导出材料分类主列表
     */
    // @RequiresPermissions("archives:mtrClass:export")
    @Log(title = "材料分类主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MtrClass mtrClass) {
        List<MtrClass> list = mtrClassService.selectMtrClassList(mtrClass);
        ExcelUtil<MtrClass> util = new ExcelUtil<MtrClass>(MtrClass.class);
        util.exportExcel(response, list, "材料分类主数据");
    }

    /**
     * 获取材料分类主详细信息
     */
    // @RequiresPermissions("archives:mtrClass:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(mtrClassService.selectMtrClassById(id));
    }

    /**
     * 新增材料分类主
     */
    // @RequiresPermissions("archives:mtrClass:add")
    @Log(title = "材料分类主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MtrClass mtrClass) {
        return toAjax(mtrClassService.insertMtrClass(mtrClass));
    }


    /**
     * 获取材料分类主详细信息
     */
    // @RequiresPermissions("archives:mtrClass:query")
    @GetMapping(value = "/initCode")
    public AjaxResult initCode(MtrClass mtrClass) {
        return success(mtrClassService.initCode(mtrClass));
    }

    @GetMapping("/getMtrClassTree")
    public List<MaterialTypeTree> getMtrClassTree(MtrClass mtrClass) {
        return mtrClassService.getMtrClassTree(mtrClass);
    }

    /**
     * 修改材料分类主
     */
    // @RequiresPermissions("archives:mtrClass:edit")
    @Log(title = "材料分类主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MtrClass mtrClass) {
        return toAjax(mtrClassService.updateMtrClass(mtrClass));
    }

    /**
     * 删除材料分类主
     */
    // @RequiresPermissions("archives:mtrClass:remove")
    @Log(title = "材料分类主", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(mtrClassService.deleteMtrClassByIds(ids));
    }
}
