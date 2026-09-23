package com.zhaocai.archives.main.controller;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import com.zhaocai.archives.dossier.domain.SubcontractingDetails;
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
import com.zhaocai.archives.main.domain.MajorSubcontractingArchives;
import com.zhaocai.archives.main.service.IMajorSubcontractingArchivesService;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.page.TableDataInfo;

/**
 * 专业分包档案主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/majorArchives")
public class MajorSubcontractingArchivesController extends BaseController
{
    @Autowired
    private IMajorSubcontractingArchivesService majorSubcontractingArchivesService;

    /**
     * 查询专业分包档案主列表
     */
    // @RequiresPermissions("archives:majorArchives:list")
    @GetMapping("/list")
    public TableDataInfo list(MajorSubcontractingArchives majorSubcontractingArchives)
    {
        startPage();
        List<MajorSubcontractingArchives> list = majorSubcontractingArchivesService.selectMajorSubcontractingArchivesList(majorSubcontractingArchives);
        return getDataTable(list);
    }

    /**
     * 导出专业分包档案主列表
     */
    // @RequiresPermissions("archives:majorArchives:export")
    @Log(title = "专业分包档案主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MajorSubcontractingArchives majorSubcontractingArchives)
    {
        List<MajorSubcontractingArchives> list = majorSubcontractingArchivesService.selectMajorSubcontractingArchivesList(majorSubcontractingArchives);
        ExcelUtil<MajorSubcontractingArchives> util = new ExcelUtil<MajorSubcontractingArchives>(MajorSubcontractingArchives.class);
        util.exportExcel(response, list, "专业分包档案主数据");
    }

    /**
     * 获取专业分包档案主详细信息
     */
    // @RequiresPermissions("archives:majorArchives:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(majorSubcontractingArchivesService.selectMajorSubcontractingArchivesById(id));
    }

    /**
     * 新增专业分包档案主
     */
    // @RequiresPermissions("archives:majorArchives:add")
    @Log(title = "专业分包档案主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MajorSubcontractingArchives majorSubcontractingArchives)
    {
        return toAjax(majorSubcontractingArchivesService.insertMajorSubcontractingArchives(majorSubcontractingArchives));
    }

    /**
     * 修改专业分包档案主
     */
    // @RequiresPermissions("archives:majorArchives:edit")
    @Log(title = "专业分包档案主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MajorSubcontractingArchives majorSubcontractingArchives)
    {
        return toAjax(majorSubcontractingArchivesService.updateMajorSubcontractingArchives(majorSubcontractingArchives));
    }

    /**
     * 删除专业分包档案主
     */
    // @RequiresPermissions("archives:majorArchives:remove")
    @Log(title = "专业分包档案主", businessType = BusinessType.DELETE)
	@PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(majorSubcontractingArchivesService.deleteMajorSubcontractingArchivesByIds(ids));
    }


    /**
     * 新增具体档案生成编号
     */
    // @RequiresPermissions("archives:materialType:add")
    @GetMapping("/initDetails")
    public AjaxResult initDetails(MajorSubcontractingArchives majorSubcontractingArchives) {
        return success(majorSubcontractingArchivesService.initDetails(majorSubcontractingArchives));
    }


}
