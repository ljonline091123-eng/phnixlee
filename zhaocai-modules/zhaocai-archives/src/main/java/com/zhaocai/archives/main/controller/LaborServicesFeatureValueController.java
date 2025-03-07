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
import com.zhaocai.archives.main.domain.LaborServicesFeatureValue;
import com.zhaocai.archives.main.service.ILaborServicesFeatureValueService;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.page.TableDataInfo;

/**
 * 劳务特征值主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/laborValue")
public class LaborServicesFeatureValueController extends BaseController
{
    @Autowired
    private ILaborServicesFeatureValueService laborServicesFeatureValueService;

    /**
     * 查询劳务特征值主列表
     */
    // @RequiresPermissions("archives:laborValue:list")
    @GetMapping("/list")
    public TableDataInfo list(LaborServicesFeatureValue laborServicesFeatureValue)
    {
//        startPage();
        List<LaborServicesFeatureValue> list = laborServicesFeatureValueService.selectLaborServicesFeatureValueList(laborServicesFeatureValue);
        return getDataTable(list);
    }

    /**
     * 导出劳务特征值主列表
     */
    // @RequiresPermissions("archives:laborValue:export")
    @Log(title = "劳务特征值主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LaborServicesFeatureValue laborServicesFeatureValue)
    {
        List<LaborServicesFeatureValue> list = laborServicesFeatureValueService.selectLaborServicesFeatureValueList(laborServicesFeatureValue);
        ExcelUtil<LaborServicesFeatureValue> util = new ExcelUtil<LaborServicesFeatureValue>(LaborServicesFeatureValue.class);
        util.exportExcel(response, list, "劳务特征值主数据");
    }

    /**
     * 获取劳务特征值主详细信息
     */
    // @RequiresPermissions("archives:laborValue:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(laborServicesFeatureValueService.selectLaborServicesFeatureValueById(id));
    }

    /**
     * 新增劳务特征值主
     */
    // @RequiresPermissions("archives:laborValue:add")
    @Log(title = "劳务特征值主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LaborServicesFeatureValue laborServicesFeatureValue)
    {
        return toAjax(laborServicesFeatureValueService.insertLaborServicesFeatureValue(laborServicesFeatureValue));
    }

    /**
     * 修改劳务特征值主
     */
    // @RequiresPermissions("archives:laborValue:edit")
    @Log(title = "劳务特征值主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LaborServicesFeatureValue laborServicesFeatureValue)
    {
        return toAjax(laborServicesFeatureValueService.updateLaborServicesFeatureValue(laborServicesFeatureValue));
    }

    /**
     * 删除劳务特征值主
     */
    // @RequiresPermissions("archives:laborValue:remove")
    @Log(title = "劳务特征值主", businessType = BusinessType.DELETE)
	@PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(laborServicesFeatureValueService.deleteLaborServicesFeatureValueByIds(ids));
    }
}
