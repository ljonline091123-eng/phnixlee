package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.DeviceItem;
import com.zhaocai.archives.dossier.service.IDeviceItemService;
import com.zhaocai.common.core.utils.StringUtils;
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
 * 设备特征项Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/deviceItem")
public class DeviceItemController extends BaseController {
    @Autowired
    private IDeviceItemService deviceItemService;

    /**
     * 查询设备特征项列表
     */
    // @RequiresPermissions("archives:deviceItem:list")
    @GetMapping("/list")
    public TableDataInfo list(DeviceItem deviceItem) {
//        startPage();
        List<DeviceItem> list = deviceItemService.selectDeviceItemList(deviceItem);
        return getDataTable(list);
    }

    /**
     * 初始化材料特征项列表
     */
    // @RequiresPermissions("archives:materialItem:list")
    @GetMapping("/initData")
    public AjaxResult initData(DeviceItem deviceItem) {
        if (StringUtils.isEmpty(deviceItem.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        deviceItemService.initData(deviceItem);
        return toAjax(true);
    }

    /**
     * 导出设备特征项列表
     */
    // @RequiresPermissions("archives:deviceItem:export")
    @Log(title = "设备特征项", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DeviceItem deviceItem) {
        List<DeviceItem> list = deviceItemService.selectDeviceItemList(deviceItem);
        ExcelUtil<DeviceItem> util = new ExcelUtil<DeviceItem>(DeviceItem.class);
        util.exportExcel(response, list, "设备特征项数据");
    }

    /**
     * 获取设备特征项详细信息
     */
    // @RequiresPermissions("archives:deviceItem:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(deviceItemService.selectDeviceItemById(id));
    }

    /**
     * 新增设备特征项
     */
    // @RequiresPermissions("archives:deviceItem:add")
    @Log(title = "设备特征项", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody DeviceItem deviceItem) {
        return toAjax(deviceItemService.insertDeviceItem(deviceItem));
    }

    /**
     * 修改设备特征项
     */
    // @RequiresPermissions("archives:deviceItem:edit")
    @Log(title = "设备特征项", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DeviceItem deviceItem) {
        return toAjax(deviceItemService.updateDeviceItem(deviceItem));
    }

    /**
     * 删除设备特征项
     */
    // @RequiresPermissions("archives:deviceItem:remove")
    @Log(title = "设备特征项", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(deviceItemService.deleteDeviceItemByIds(ids));
    }


    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody DeviceItem materialType) {
        int i = deviceItemService.addToMain(materialType);
        if(i == -1){
            return  AjaxResult.success("编码已存在");
        }
        return toAjax(i);
    }

    /**
     * 关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/associationToMain")
    public AjaxResult associationToMain(@RequestBody DeviceItem materialType) {
        return toAjax(deviceItemService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody DeviceItem materialType) {
        return toAjax(deviceItemService.unAssociationToMain(materialType));
    }


}
