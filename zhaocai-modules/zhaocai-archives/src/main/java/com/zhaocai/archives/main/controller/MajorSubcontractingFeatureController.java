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
import com.zhaocai.archives.main.domain.MajorSubcontractingFeature;
import com.zhaocai.archives.main.service.IMajorSubcontractingFeatureService;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.page.TableDataInfo;

/**
 * 专业分包特征项主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/majorFeature")
public class MajorSubcontractingFeatureController extends BaseController
{
    @Autowired
    private IMajorSubcontractingFeatureService majorSubcontractingFeatureService;

    /**
     * 查询专业分包特征项主列表
     */
    // @RequiresPermissions("archives:majorFeature:list")
    @GetMapping("/list")
    public TableDataInfo list(MajorSubcontractingFeature majorSubcontractingFeature)
    {
//        startPage();
        List<MajorSubcontractingFeature> list = majorSubcontractingFeatureService.selectMajorSubcontractingFeatureList(majorSubcontractingFeature);
        return getDataTable(list);
    }

    /**
     * 导出专业分包特征项主列表
     */
    // @RequiresPermissions("archives:majorFeature:export")
    @Log(title = "专业分包特征项主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MajorSubcontractingFeature majorSubcontractingFeature)
    {
        List<MajorSubcontractingFeature> list = majorSubcontractingFeatureService.selectMajorSubcontractingFeatureList(majorSubcontractingFeature);
        ExcelUtil<MajorSubcontractingFeature> util = new ExcelUtil<MajorSubcontractingFeature>(MajorSubcontractingFeature.class);
        util.exportExcel(response, list, "专业分包特征项主数据");
    }

    /**
     * 获取专业分包特征项主详细信息
     */
    // @RequiresPermissions("archives:majorFeature:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(majorSubcontractingFeatureService.selectMajorSubcontractingFeatureById(id));
    }

    /**
     * 新增专业分包特征项主
     */
    // @RequiresPermissions("archives:majorFeature:add")
    @Log(title = "专业分包特征项主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MajorSubcontractingFeature majorSubcontractingFeature)
    {
        return toAjax(majorSubcontractingFeatureService.insertMajorSubcontractingFeature(majorSubcontractingFeature));
    }

    /**
     * 修改专业分包特征项主
     */
    // @RequiresPermissions("archives:majorFeature:edit")
    @Log(title = "专业分包特征项主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MajorSubcontractingFeature majorSubcontractingFeature)
    {
        return toAjax(majorSubcontractingFeatureService.updateMajorSubcontractingFeature(majorSubcontractingFeature));
    }

    /**
     * 删除专业分包特征项主
     */
    // @RequiresPermissions("archives:majorFeature:remove")
    @Log(title = "专业分包特征项主", businessType = BusinessType.DELETE)
	@PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(majorSubcontractingFeatureService.deleteMajorSubcontractingFeatureByIds(ids));
    }
}
