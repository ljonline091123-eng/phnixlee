package com.zhaocai.archives.main.controller;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import com.zhaocai.common.security.annotation.RequiresPermissions;
import com.zhaocai.archives.main.domain.MtrArchives;
import com.zhaocai.archives.main.service.IMtrArchivesService;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.page.TableDataInfo;

/**
 * 材料档案主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/mtrArchives")
public class MtrArchivesController extends BaseController
{
    @Autowired
    private IMtrArchivesService mtrArchivesService;

    /**
     * 查询材料档案主列表
     */
    // @RequiresPermissions("archives:mtrArchives:list")
    @GetMapping("/list")
    public TableDataInfo list(MtrArchives mtrArchives)
    {
        startPage();
        List<MtrArchives> list = mtrArchivesService.selectMtrArchivesList(mtrArchives);
        return getDataTable(list);
    }

    /**
     * 导出材料档案主列表
     */
    // @RequiresPermissions("archives:mtrArchives:export")
    @Log(title = "材料档案主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MtrArchives mtrArchives)
    {
        List<MtrArchives> list = mtrArchivesService.selectMtrArchivesList(mtrArchives);
        ExcelUtil<MtrArchives> util = new ExcelUtil<MtrArchives>(MtrArchives.class);
        util.exportExcel(response, list, "材料档案主数据");
    }

    /**
     * 获取材料档案主详细信息
     */
    // @RequiresPermissions("archives:mtrArchives:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mtrArchivesService.selectMtrArchivesById(id));
    }

    /**
     * 新增材料档案主
     */
    // @RequiresPermissions("archives:mtrArchives:add")
    @Log(title = "材料档案主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MtrArchives mtrArchives)
    {
        return toAjax(mtrArchivesService.insertMtrArchives(mtrArchives));
    }

    /**
     * 修改材料档案主
     */
    // @RequiresPermissions("archives:mtrArchives:edit")
    @Log(title = "材料档案主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MtrArchives mtrArchives)
    {
        return toAjax(mtrArchivesService.updateMtrArchives(mtrArchives));
    }

    /**
     * 删除材料档案主
     */
    // @RequiresPermissions("archives:mtrArchives:remove")
    @Log(title = "材料档案主", businessType = BusinessType.DELETE)
	@PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mtrArchivesService.deleteMtrArchivesByIds(ids));
    }
}
