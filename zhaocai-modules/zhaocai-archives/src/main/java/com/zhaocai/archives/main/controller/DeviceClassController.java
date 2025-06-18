package com.zhaocai.archives.main.controller;

import com.zhaocai.archives.dossier.tree.DeviceTypeTree;
import com.zhaocai.archives.main.domain.DeviceClass;
import com.zhaocai.archives.main.domain.DeviceClassExcelData;
import com.zhaocai.archives.main.service.IDeviceClassService;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.web.page.TableDataInfo;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 设备分类主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/deviceClass")
public class DeviceClassController extends BaseController {
    @Autowired
    private IDeviceClassService deviceClassService;

    /**
     * 查询设备分类主列表
     */
    // @RequiresPermissions("archives:deviceClass:list")
    @GetMapping("/list")
    public TableDataInfo list(DeviceClass deviceClass) {
        startPage();
        List<DeviceClass> list = deviceClassService.selectDeviceClassList(deviceClass);
        return getDataTable(list);
    }

    /**
     * 导出设备分类主列表
     */
    // @RequiresPermissions("archives:deviceClass:export")
    @Log(title = "设备分类主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DeviceClass deviceClass) {
        List<DeviceClass> list = deviceClassService.selectDeviceClassList(deviceClass);
        ExcelUtil<DeviceClass> util = new ExcelUtil<DeviceClass>(DeviceClass.class);
        util.exportExcel(response, list, "设备分类主数据");
    }

    /**
     * 获取设备分类主详细信息
     */
    // @RequiresPermissions("archives:deviceClass:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(deviceClassService.selectDeviceClassById(id));
    }

    /**
     * 新增设备分类主
     */
    // @RequiresPermissions("archives:deviceClass:add")
    @Log(title = "设备分类主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody DeviceClass deviceClass) {
        return toAjax(deviceClassService.insertDeviceClass(deviceClass));
    }

    /**
     * 修改设备分类主
     */
    // @RequiresPermissions("archives:deviceClass:edit")
    @Log(title = "设备分类主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DeviceClass deviceClass) {
        return toAjax(deviceClassService.updateDeviceClass(deviceClass));
    }

    /**
     * 删除设备分类主
     */
    // @RequiresPermissions("archives:deviceClass:remove")
    @Log(title = "设备分类主", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(deviceClassService.deleteDeviceClassByIds(ids));
    }


    /**
     * 树
     *
     * @return
     */
    @GetMapping("/getDeviceClassTree")
    public List<DeviceTypeTree> getDeviceClassTree() {
        return deviceClassService.getDeviceClassTree();
    }

    /**
     * 获取材料分类主详细信息
     */
    // @RequiresPermissions("archives:mtrClass:query")
    @GetMapping(value = "/initCode")
    public AjaxResult initCode(DeviceClass deviceClass) {
        return success(deviceClassService.initCode(deviceClass));
    }


    @Log(title = "设备导入", businessType = BusinessType.IMPORT)
//    @RequiresPermissions("system:user:import")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<DeviceClassExcelData> util = new ExcelUtil<DeviceClassExcelData>(DeviceClassExcelData.class);
        List<DeviceClassExcelData> userList = util.importExcel(file.getInputStream());
        String operName = SecurityUtils.getUsername();
        String message = deviceClassService.importData(userList, updateSupport, operName);
        return success(message);
    }


    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<DeviceClassExcelData> util = new ExcelUtil<DeviceClassExcelData>(DeviceClassExcelData.class);
        util.importTemplateExcel(response, "设备导入");
    }


}
