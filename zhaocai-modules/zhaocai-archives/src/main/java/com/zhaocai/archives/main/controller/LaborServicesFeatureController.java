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
import com.zhaocai.archives.main.domain.LaborServicesFeature;
import com.zhaocai.archives.main.service.ILaborServicesFeatureService;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.page.TableDataInfo;

/**
 * 劳务特征项主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/laborFeature")
public class LaborServicesFeatureController extends BaseController
{
    @Autowired
    private ILaborServicesFeatureService laborServicesFeatureService;

    /**
     * 查询劳务特征项主列表
     */
    // @RequiresPermissions("archives:laborFeature:list")
    @GetMapping("/list")
    public TableDataInfo list(LaborServicesFeature laborServicesFeature)
    {
//        startPage();
        List<LaborServicesFeature> list = laborServicesFeatureService.selectLaborServicesFeatureList(laborServicesFeature);
        return getDataTable(list);
    }

    /**
     * 导出劳务特征项主列表
     */
    // @RequiresPermissions("archives:laborFeature:export")
    @Log(title = "劳务特征项主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LaborServicesFeature laborServicesFeature)
    {
        List<LaborServicesFeature> list = laborServicesFeatureService.selectLaborServicesFeatureList(laborServicesFeature);
        ExcelUtil<LaborServicesFeature> util = new ExcelUtil<LaborServicesFeature>(LaborServicesFeature.class);
        util.exportExcel(response, list, "劳务特征项主数据");
    }

    /**
     * 获取劳务特征项主详细信息
     */
    // @RequiresPermissions("archives:laborFeature:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(laborServicesFeatureService.selectLaborServicesFeatureById(id));
    }

    /**
     * 新增劳务特征项主
     */
    // @RequiresPermissions("archives:laborFeature:add")
    @Log(title = "劳务特征项主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LaborServicesFeature laborServicesFeature)
    {
        return toAjax(laborServicesFeatureService.insertLaborServicesFeature(laborServicesFeature));
    }

    /**
     * 修改劳务特征项主
     */
    // @RequiresPermissions("archives:laborFeature:edit")
    @Log(title = "劳务特征项主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LaborServicesFeature laborServicesFeature)
    {
        return toAjax(laborServicesFeatureService.updateLaborServicesFeature(laborServicesFeature));
    }

    /**
     * 删除劳务特征项主
     */
    // @RequiresPermissions("archives:laborFeature:remove")
    @Log(title = "劳务特征项主", businessType = BusinessType.DELETE)
	@PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(laborServicesFeatureService.deleteLaborServicesFeatureByIds(ids));
    }
}
