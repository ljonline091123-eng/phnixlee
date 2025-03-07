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
import com.zhaocai.archives.main.domain.DeviceFeature;
import com.zhaocai.archives.main.service.IDeviceFeatureService;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.page.TableDataInfo;

/**
 * 设备特征项主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/deviceFeature")
public class DeviceFeatureController extends BaseController
{
    @Autowired
    private IDeviceFeatureService deviceFeatureService;

    /**
     * 查询设备特征项主列表
     */
    // @RequiresPermissions("archives:deviceFeature:list")
    @GetMapping("/list")
    public TableDataInfo list(DeviceFeature deviceFeature)
    {
//        startPage();
        List<DeviceFeature> list = deviceFeatureService.selectDeviceFeatureList(deviceFeature);
        return getDataTable(list);
    }

    /**
     * 导出设备特征项主列表
     */
    // @RequiresPermissions("archives:deviceFeature:export")
    @Log(title = "设备特征项主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DeviceFeature deviceFeature)
    {
        List<DeviceFeature> list = deviceFeatureService.selectDeviceFeatureList(deviceFeature);
        ExcelUtil<DeviceFeature> util = new ExcelUtil<DeviceFeature>(DeviceFeature.class);
        util.exportExcel(response, list, "设备特征项主数据");
    }

    /**
     * 获取设备特征项主详细信息
     */
    // @RequiresPermissions("archives:deviceFeature:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(deviceFeatureService.selectDeviceFeatureById(id));
    }

    /**
     * 新增设备特征项主
     */
    // @RequiresPermissions("archives:deviceFeature:add")
    @Log(title = "设备特征项主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody DeviceFeature deviceFeature)
    {
        return toAjax(deviceFeatureService.insertDeviceFeature(deviceFeature));
    }

    /**
     * 修改设备特征项主
     */
    // @RequiresPermissions("archives:deviceFeature:edit")
    @Log(title = "设备特征项主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DeviceFeature deviceFeature)
    {
        return toAjax(deviceFeatureService.updateDeviceFeature(deviceFeature));
    }

    /**
     * 删除设备特征项主
     */
    // @RequiresPermissions("archives:deviceFeature:remove")
    @Log(title = "设备特征项主", businessType = BusinessType.DELETE)
	@PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(deviceFeatureService.deleteDeviceFeatureByIds(ids));
    }
}
