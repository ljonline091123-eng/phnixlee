package com.zhaocai.archives.main.controller;

import com.zhaocai.archives.main.domain.MajorSubcontractingFeatureValue;
import com.zhaocai.archives.main.service.IMajorSubcontractingFeatureValueService;
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
 * 专业分包特征值主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/majorValue")
public class MajorSubcontractingFeatureValueController extends BaseController {
    @Autowired
    private IMajorSubcontractingFeatureValueService majorSubcontractingFeatureValueService;

    /**
     * 查询专业分包特征值主列表
     */
    // @RequiresPermissions("archives:majorValue:list")
    @GetMapping("/list")
    public TableDataInfo list(MajorSubcontractingFeatureValue majorSubcontractingFeatureValue) {
//        startPage();
        List<MajorSubcontractingFeatureValue> list = majorSubcontractingFeatureValueService.selectMajorSubcontractingFeatureValueList(majorSubcontractingFeatureValue);
        return getDataTable(list);
    }

    /**
     * 导出专业分包特征值主列表
     */
    // @RequiresPermissions("archives:majorValue:export")
    @Log(title = "专业分包特征值主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MajorSubcontractingFeatureValue majorSubcontractingFeatureValue) {
        List<MajorSubcontractingFeatureValue> list = majorSubcontractingFeatureValueService.selectMajorSubcontractingFeatureValueList(majorSubcontractingFeatureValue);
        ExcelUtil<MajorSubcontractingFeatureValue> util = new ExcelUtil<MajorSubcontractingFeatureValue>(MajorSubcontractingFeatureValue.class);
        util.exportExcel(response, list, "专业分包特征值主数据");
    }

    /**
     * 获取专业分包特征值主详细信息
     */
    // @RequiresPermissions("archives:majorValue:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(majorSubcontractingFeatureValueService.selectMajorSubcontractingFeatureValueById(id));
    }

    /**
     * 新增专业分包特征值主
     */
    // @RequiresPermissions("archives:majorValue:add")
    @Log(title = "专业分包特征值主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MajorSubcontractingFeatureValue majorSubcontractingFeatureValue) {
        return toAjax(majorSubcontractingFeatureValueService.insertMajorSubcontractingFeatureValue(majorSubcontractingFeatureValue));
    }

    /**
     * 修改专业分包特征值主
     */
    // @RequiresPermissions("archives:majorValue:edit")
    @Log(title = "专业分包特征值主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MajorSubcontractingFeatureValue majorSubcontractingFeatureValue) {
        return toAjax(majorSubcontractingFeatureValueService.updateMajorSubcontractingFeatureValue(majorSubcontractingFeatureValue));
    }

    /**
     * 删除专业分包特征值主
     */
    // @RequiresPermissions("archives:majorValue:remove")
    @Log(title = "专业分包特征值主", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(majorSubcontractingFeatureValueService.deleteMajorSubcontractingFeatureValueByIds(ids));
    }
}
