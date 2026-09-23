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
import com.zhaocai.archives.main.domain.DeviceFeatureValue;
import com.zhaocai.archives.main.service.IDeviceFeatureValueService;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.page.TableDataInfo;

/**
 * 设备特征值主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/featureValue")
public class DeviceFeatureValueController extends BaseController
{
    @Autowired
    private IDeviceFeatureValueService deviceFeatureValueService;

    /**
     * 查询设备特征值主列表
     */
    // @RequiresPermissions("archives:featureValue:list")
    @GetMapping("/list")
    public TableDataInfo list(DeviceFeatureValue deviceFeatureValue)
    {
//        startPage();
        List<DeviceFeatureValue> list = deviceFeatureValueService.selectDeviceFeatureValueList(deviceFeatureValue);
        return getDataTable(list);
    }

    /**
     * 导出设备特征值主列表
     */
    // @RequiresPermissions("archives:featureValue:export")
    @Log(title = "设备特征值主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DeviceFeatureValue deviceFeatureValue)
    {
        List<DeviceFeatureValue> list = deviceFeatureValueService.selectDeviceFeatureValueList(deviceFeatureValue);
        ExcelUtil<DeviceFeatureValue> util = new ExcelUtil<DeviceFeatureValue>(DeviceFeatureValue.class);
        util.exportExcel(response, list, "设备特征值主数据");
    }

    /**
     * 获取设备特征值主详细信息
     */
    // @RequiresPermissions("archives:featureValue:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(deviceFeatureValueService.selectDeviceFeatureValueById(id));
    }

    /**
     * 新增设备特征值主
     */
    // @RequiresPermissions("archives:featureValue:add")
    @Log(title = "设备特征值主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody DeviceFeatureValue deviceFeatureValue)
    {
        return toAjax(deviceFeatureValueService.insertDeviceFeatureValue(deviceFeatureValue));
    }

    /**
     * 修改设备特征值主
     */
    // @RequiresPermissions("archives:featureValue:edit")
    @Log(title = "设备特征值主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DeviceFeatureValue deviceFeatureValue)
    {
        return toAjax(deviceFeatureValueService.updateDeviceFeatureValue(deviceFeatureValue));
    }

    /**
     * 删除设备特征值主
     */
    // @RequiresPermissions("archives:featureValue:remove")
    @Log(title = "设备特征值主", businessType = BusinessType.DELETE)
	@PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(deviceFeatureValueService.deleteDeviceFeatureValueByIds(ids));
    }
}
