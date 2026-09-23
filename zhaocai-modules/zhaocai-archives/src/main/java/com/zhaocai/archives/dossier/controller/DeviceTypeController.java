package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.DeviceType;
import com.zhaocai.archives.dossier.service.IDeviceTypeService;
import com.zhaocai.archives.dossier.tree.DeviceTypeTree;
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
import java.util.Map;

/**
 * 设备分类Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/deviceType")
public class DeviceTypeController extends BaseController {
    @Autowired
    private IDeviceTypeService deviceTypeService;

    /**
     * 查询设备分类列表
     */
    // @RequiresPermissions("archives:deviceType:list")
    @GetMapping("/list")
    public TableDataInfo list(DeviceType deviceType) {
        startPage();
        List<DeviceType> list = deviceTypeService.selectDeviceTypeList(deviceType);
        return getDataTable(list);
    }

    /**
     * 初始化
     */
    // @RequiresPermissions("archives:deviceType:list")
    @GetMapping("/initData")
    public AjaxResult initData(DeviceType deviceType) {
        if (StringUtils.isEmpty(deviceType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        deviceTypeService.initData(deviceType);
        return toAjax(true);
    }


    /**
     * 获取材料分类树列表
     *
     * @param deviceType
     * @return
     */
    @GetMapping("/getDeviceTypeTree")
    public List<DeviceTypeTree> getDeviceTypeTree(DeviceType deviceType) {
        return deviceTypeService.getDeviceTypeTree(deviceType);
    }


    /**
     * 新增材料类型生成编号
     */
    // @RequiresPermissions("archives:materialType:add")
    @GetMapping("/initDeviceType")
    public AjaxResult initDeviceType(DeviceType deviceType) {
        return success(deviceTypeService.initDeviceType(deviceType));
    }


    /**
     * 导出设备分类列表
     */
    // @RequiresPermissions("archives:deviceType:export")
    @Log(title = "设备分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DeviceType deviceType) {
        List<DeviceType> list = deviceTypeService.selectDeviceTypeList(deviceType);
        ExcelUtil<DeviceType> util = new ExcelUtil<DeviceType>(DeviceType.class);
        util.exportExcel(response, list, "设备分类数据");
    }

    /**
     * 获取设备分类详细信息
     */
    // @RequiresPermissions("archives:deviceType:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(deviceTypeService.selectDeviceTypeById(id));
    }

    /**
     * 新增设备分类
     */
    // @RequiresPermissions("archives:deviceType:add")
    @Log(title = "设备分类", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody DeviceType deviceType) {
        return toAjax(deviceTypeService.insertDeviceType(deviceType));
    }

    /**
     * 修改设备分类
     */
    // @RequiresPermissions("archives:deviceType:edit")
    @Log(title = "设备分类", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DeviceType deviceType) {
        return toAjax(deviceTypeService.updateDeviceType(deviceType));
    }

    /**
     * 删除设备分类
     */
    // @RequiresPermissions("archives:deviceType:remove")
    @Log(title = "设备分类", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(deviceTypeService.deleteDeviceTypeByIds(ids));
    }

    /**
     * 审核通过
     *
     * @param variables
     */
    @PostMapping("/processAuditPass")
    public AjaxResult processAuditPass(@RequestBody Map<String, Object> variables) {
        deviceTypeService.processAuditPass(variables);
        return success();
    }
//
//    /**
//     * 审核通过
//     *
//     * @param variables
//     */
//    @PostMapping("/processAuditPass")
//    public void processAuditPass(@RequestBody Map<String, Object> variables) {
//        deviceTypeService.processAuditPass(variables);
//    }


    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody DeviceType materialType) {
        return toAjax(deviceTypeService.addToMain(materialType));
    }

    /**
     * 关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/associationToMain")
    public AjaxResult associationToMain(@RequestBody DeviceType materialType) {
        return toAjax(deviceTypeService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody DeviceType materialType) {
        return toAjax(deviceTypeService.unAssociationToMain(materialType));
    }


}
