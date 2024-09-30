package com.zhaocai.business.pub.controller;

import com.zhaocai.business.pub.domain.IndexStatistic;
import com.zhaocai.business.pub.service.IIndexStatisticService;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.web.page.TableDataInfo;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import com.zhaocai.common.security.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 首页统计数据Controller
 * 
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/statistic")
public class IndexStatisticController extends BaseController
{
    @Autowired
    private IIndexStatisticService indexStatisticService;

    /**
     * 查询首页统计数据列表
     */
    @RequiresPermissions("pub:statistic:list")
    @GetMapping("/list")
    public TableDataInfo list(IndexStatistic indexStatistic)
    {
        startPage();
        List<IndexStatistic> list = indexStatisticService.selectIndexStatisticList(indexStatistic);
        return getDataTable(list);
    }

    /**
     * 导出首页统计数据列表
     */
    @RequiresPermissions("pub:statistic:export")
    @Log(title = "首页统计数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, IndexStatistic indexStatistic)
    {
        List<IndexStatistic> list = indexStatisticService.selectIndexStatisticList(indexStatistic);
        ExcelUtil<IndexStatistic> util = new ExcelUtil<IndexStatistic>(IndexStatistic.class);
        util.exportExcel(response, list, "首页统计数据数据");
    }

    /**
     * 获取首页统计数据详细信息
     */
    @RequiresPermissions("pub:statistic:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(indexStatisticService.selectIndexStatisticById(id));
    }

    /**
     * 新增首页统计数据
     */
    @RequiresPermissions("pub:statistic:add")
    @Log(title = "首页统计数据", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody IndexStatistic indexStatistic)
    {
        return toAjax(indexStatisticService.insertIndexStatistic(indexStatistic));
    }

    /**
     * 修改首页统计数据
     */
    @RequiresPermissions("pub:statistic:edit")
    @Log(title = "首页统计数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody IndexStatistic indexStatistic)
    {
        return toAjax(indexStatisticService.updateIndexStatistic(indexStatistic));
    }

    /**
     * 删除首页统计数据
     */
    @RequiresPermissions("pub:statistic:remove")
    @Log(title = "首页统计数据", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(indexStatisticService.deleteIndexStatisticByIds(ids));
    }
}
