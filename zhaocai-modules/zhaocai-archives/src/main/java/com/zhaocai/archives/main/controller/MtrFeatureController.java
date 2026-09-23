package com.zhaocai.archives.main.controller;

import com.zhaocai.archives.main.domain.MtrFeature;
import com.zhaocai.archives.main.service.IMtrFeatureService;
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
 * 材料特征项主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/mtrFeature")
public class MtrFeatureController extends BaseController {
    @Autowired
    private IMtrFeatureService mtrFeatureService;

    /**
     * 查询材料特征项主列表
     */
    // @RequiresPermissions("archives:mtrFeature:list")
    @GetMapping("/list")
    public TableDataInfo list(MtrFeature mtrFeature) {
//        startPage();
        List<MtrFeature> list = mtrFeatureService.selectMtrFeatureList(mtrFeature);
        return getDataTable(list);
    }

    /**
     * 导出材料特征项主列表
     */
    // @RequiresPermissions("archives:mtrFeature:export")
    @Log(title = "材料特征项主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MtrFeature mtrFeature) {
        List<MtrFeature> list = mtrFeatureService.selectMtrFeatureList(mtrFeature);
        ExcelUtil<MtrFeature> util = new ExcelUtil<MtrFeature>(MtrFeature.class);
        util.exportExcel(response, list, "材料特征项主数据");
    }

    /**
     * 获取材料特征项主详细信息
     */
    // @RequiresPermissions("archives:mtrFeature:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(mtrFeatureService.selectMtrFeatureById(id));
    }

    /**
     * 新增材料特征项主
     */
    // @RequiresPermissions("archives:mtrFeature:add")
    @Log(title = "材料特征项主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MtrFeature mtrFeature) {
        return toAjax(mtrFeatureService.insertMtrFeature(mtrFeature));
    }

    /**
     * 修改材料特征项主
     */
    // @RequiresPermissions("archives:mtrFeature:edit")
    @Log(title = "材料特征项主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MtrFeature mtrFeature) {
        return toAjax(mtrFeatureService.updateMtrFeature(mtrFeature));
    }

    /**
     * 删除材料特征项主
     */
    // @RequiresPermissions("archives:mtrFeature:remove")
    @Log(title = "材料特征项主", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(mtrFeatureService.deleteMtrFeatureByIds(ids));
    }
}
