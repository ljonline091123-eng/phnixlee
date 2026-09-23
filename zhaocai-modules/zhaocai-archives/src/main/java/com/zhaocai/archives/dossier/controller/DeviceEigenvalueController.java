package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.DeviceEigenvalue;
import com.zhaocai.archives.dossier.service.IDeviceEigenvalueService;
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
 * 设备特征值Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/deviceEigenvalue")
public class DeviceEigenvalueController extends BaseController {
    @Autowired
    private IDeviceEigenvalueService deviceEigenvalueService;

    /**
     * 查询设备特征值列表
     */
    // @RequiresPermissions("archives:deviceEigenvalue:list")
    @GetMapping("/list")
    public TableDataInfo list(DeviceEigenvalue deviceEigenvalue) {
//        startPage();
        List<DeviceEigenvalue> list = deviceEigenvalueService.selectDeviceEigenvalueList(deviceEigenvalue);
        return getDataTable(list);
    }


    /**
     * 初始化材料特征项列表
     */
    // @RequiresPermissions("archives:materialItem:list")
    @GetMapping("/initData")
    public AjaxResult initData(DeviceEigenvalue deviceEigenvalue) {
        if (StringUtils.isEmpty(deviceEigenvalue.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        deviceEigenvalueService.initData(deviceEigenvalue);
        return toAjax(true);
    }


    /**
     * 导出设备特征值列表
     */
    // @RequiresPermissions("archives:deviceEigenvalue:export")
    @Log(title = "设备特征值", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DeviceEigenvalue deviceEigenvalue) {
        List<DeviceEigenvalue> list = deviceEigenvalueService.selectDeviceEigenvalueList(deviceEigenvalue);
        ExcelUtil<DeviceEigenvalue> util = new ExcelUtil<DeviceEigenvalue>(DeviceEigenvalue.class);
        util.exportExcel(response, list, "设备特征值数据");
    }

    /**
     * 获取设备特征值详细信息
     */
    // @RequiresPermissions("archives:deviceEigenvalue:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(deviceEigenvalueService.selectDeviceEigenvalueById(id));
    }

    /**
     * 新增设备特征值
     */
    // @RequiresPermissions("archives:deviceEigenvalue:add")
    @Log(title = "设备特征值", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody DeviceEigenvalue deviceEigenvalue) {
        return toAjax(deviceEigenvalueService.insertDeviceEigenvalue(deviceEigenvalue));
    }

    /**
     * 修改设备特征值
     */
    // @RequiresPermissions("archives:deviceEigenvalue:edit")
    @Log(title = "设备特征值", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DeviceEigenvalue deviceEigenvalue) {
        return toAjax(deviceEigenvalueService.updateDeviceEigenvalue(deviceEigenvalue));
    }

    /**
     * 删除设备特征值
     */
    // @RequiresPermissions("archives:deviceEigenvalue:remove")
    @Log(title = "设备特征值", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(deviceEigenvalueService.deleteDeviceEigenvalueByIds(ids));
    }


    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody DeviceEigenvalue materialType) {
        int i = deviceEigenvalueService.addToMain(materialType);
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
    public AjaxResult associationToMain(@RequestBody DeviceEigenvalue materialType) {
        return toAjax(deviceEigenvalueService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody DeviceEigenvalue materialType) {
        return toAjax(deviceEigenvalueService.unAssociationToMain(materialType));
    }


}
