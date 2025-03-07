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
import com.zhaocai.archives.main.domain.MtrFeatureValue;
import com.zhaocai.archives.main.service.IMtrFeatureValueService;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.page.TableDataInfo;

/**
 * 材料特征值主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/mtrValue")
public class MtrFeatureValueController extends BaseController
{
    @Autowired
    private IMtrFeatureValueService mtrFeatureValueService;

    /**
     * 查询材料特征值主列表
     */
    // @RequiresPermissions("archives:mtrValue:list")
    @GetMapping("/list")
    public TableDataInfo list(MtrFeatureValue mtrFeatureValue)
    {
//        startPage();
        List<MtrFeatureValue> list = mtrFeatureValueService.selectMtrFeatureValueList(mtrFeatureValue);
        return getDataTable(list);
    }

    /**
     * 导出材料特征值主列表
     */
    // @RequiresPermissions("archives:mtrValue:export")
    @Log(title = "材料特征值主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MtrFeatureValue mtrFeatureValue)
    {
        List<MtrFeatureValue> list = mtrFeatureValueService.selectMtrFeatureValueList(mtrFeatureValue);
        ExcelUtil<MtrFeatureValue> util = new ExcelUtil<MtrFeatureValue>(MtrFeatureValue.class);
        util.exportExcel(response, list, "材料特征值主数据");
    }

    /**
     * 获取材料特征值主详细信息
     */
    // @RequiresPermissions("archives:mtrValue:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mtrFeatureValueService.selectMtrFeatureValueById(id));
    }

    /**
     * 新增材料特征值主
     */
    // @RequiresPermissions("archives:mtrValue:add")
    @Log(title = "材料特征值主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MtrFeatureValue mtrFeatureValue)
    {
        return toAjax(mtrFeatureValueService.insertMtrFeatureValue(mtrFeatureValue));
    }

    /**
     * 修改材料特征值主
     */
    // @RequiresPermissions("archives:mtrValue:edit")
    @Log(title = "材料特征值主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MtrFeatureValue mtrFeatureValue)
    {
        return toAjax(mtrFeatureValueService.updateMtrFeatureValue(mtrFeatureValue));
    }

    /**
     * 删除材料特征值主
     */
    // @RequiresPermissions("archives:mtrValue:remove")
    @Log(title = "材料特征值主", businessType = BusinessType.DELETE)
	@PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mtrFeatureValueService.deleteMtrFeatureValueByIds(ids));
    }
}
