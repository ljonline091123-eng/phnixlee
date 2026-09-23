package com.zhaocai.archives.main.controller;

import com.zhaocai.archives.main.domain.LaborServicesArchives;
import com.zhaocai.archives.main.service.ILaborServicesArchivesService;
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
 * 劳务档案主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/laborArchives")
public class LaborServicesArchivesController extends BaseController {
    @Autowired
    private ILaborServicesArchivesService laborServicesArchivesService;

    /**
     * 查询劳务档案主列表
     */
    // @RequiresPermissions("archives:laborArchives:list")
    @GetMapping("/list")
    public TableDataInfo list(LaborServicesArchives laborServicesArchives) {
        startPage();
        List<LaborServicesArchives> list = laborServicesArchivesService.selectLaborServicesArchivesList(laborServicesArchives);
        return getDataTable(list);
    }

    /**
     * 导出劳务档案主列表
     */
    // @RequiresPermissions("archives:laborArchives:export")
    @Log(title = "劳务档案主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LaborServicesArchives laborServicesArchives) {
        List<LaborServicesArchives> list = laborServicesArchivesService.selectLaborServicesArchivesList(laborServicesArchives);
        ExcelUtil<LaborServicesArchives> util = new ExcelUtil<LaborServicesArchives>(LaborServicesArchives.class);
        util.exportExcel(response, list, "劳务档案主数据");
    }

    /**
     * 获取劳务档案主详细信息
     */
    // @RequiresPermissions("archives:laborArchives:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(laborServicesArchivesService.selectLaborServicesArchivesById(id));
    }

    /**
     * 新增劳务档案主
     */
    // @RequiresPermissions("archives:laborArchives:add")
    @Log(title = "劳务档案主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LaborServicesArchives laborServicesArchives) {
        return toAjax(laborServicesArchivesService.insertLaborServicesArchives(laborServicesArchives));
    }

    /**
     * 修改劳务档案主
     */
    // @RequiresPermissions("archives:laborArchives:edit")
    @Log(title = "劳务档案主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LaborServicesArchives laborServicesArchives) {
        return toAjax(laborServicesArchivesService.updateLaborServicesArchives(laborServicesArchives));
    }

    /**
     * 删除劳务档案主
     */
    // @RequiresPermissions("archives:laborArchives:remove")
    @Log(title = "劳务档案主", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(laborServicesArchivesService.deleteLaborServicesArchivesByIds(ids));
    }


    /**
     * 新增具体档案生成编号
     */
    // @RequiresPermissions("archives:materialType:add")
    @GetMapping("/initDetails")
    public AjaxResult initDetails(LaborServicesArchives laborServicesArchives) {
        return success(laborServicesArchivesService.initDetails(laborServicesArchives));
    }


}
