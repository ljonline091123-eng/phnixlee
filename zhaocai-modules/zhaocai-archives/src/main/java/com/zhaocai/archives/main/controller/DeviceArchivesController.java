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
import com.zhaocai.archives.main.domain.DeviceArchives;
import com.zhaocai.archives.main.service.IDeviceArchivesService;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.page.TableDataInfo;

/**
 * 设备档案主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/deviceArchives")
public class DeviceArchivesController extends BaseController
{
    @Autowired
    private IDeviceArchivesService deviceArchivesService;

    /**
     * 查询设备档案主列表
     */
    // @RequiresPermissions("archives:deviceArchives:list")
    @GetMapping("/list")
    public TableDataInfo list(DeviceArchives deviceArchives)
    {
        startPage();
        List<DeviceArchives> list = deviceArchivesService.selectDeviceArchivesList(deviceArchives);
        return getDataTable(list);
    }

    /**
     * 导出设备档案主列表
     */
    // @RequiresPermissions("archives:deviceArchives:export")
    @Log(title = "设备档案主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DeviceArchives deviceArchives)
    {
        List<DeviceArchives> list = deviceArchivesService.selectDeviceArchivesList(deviceArchives);
        ExcelUtil<DeviceArchives> util = new ExcelUtil<DeviceArchives>(DeviceArchives.class);
        util.exportExcel(response, list, "设备档案主数据");
    }

    /**
     * 获取设备档案主详细信息
     */
    // @RequiresPermissions("archives:deviceArchives:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(deviceArchivesService.selectDeviceArchivesById(id));
    }

    /**
     * 新增设备档案主
     */
    // @RequiresPermissions("archives:deviceArchives:add")
    @Log(title = "设备档案主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody DeviceArchives deviceArchives)
    {
        return toAjax(deviceArchivesService.insertDeviceArchives(deviceArchives));
    }

    /**
     * 修改设备档案主
     */
    // @RequiresPermissions("archives:deviceArchives:edit")
    @Log(title = "设备档案主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DeviceArchives deviceArchives)
    {
        return toAjax(deviceArchivesService.updateDeviceArchives(deviceArchives));
    }

    /**
     * 删除设备档案主
     */
    // @RequiresPermissions("archives:deviceArchives:remove")
    @Log(title = "设备档案主", businessType = BusinessType.DELETE)
	@PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(deviceArchivesService.deleteDeviceArchivesByIds(ids));
    }
}
