package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.DeviceDetails;
import com.zhaocai.archives.dossier.service.IDeviceDetailsService;
import com.zhaocai.common.core.utils.PageUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.web.page.PageDomain;
import com.zhaocai.common.core.web.page.TableDataInfo;
import com.zhaocai.common.core.web.page.TableSupport;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 设备详情Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/deviceDetails")
public class DeviceDetailsController extends BaseController {
    @Autowired
    private IDeviceDetailsService deviceDetailsService;

    /**
     * 查询设备详情列表
     */
    // @RequiresPermissions("archives:deviceDetails:list")
    @GetMapping("/list")
    public TableDataInfo list(DeviceDetails deviceDetails) {
        if (StringUtils.isEmpty(deviceDetails.getQueryType())) {
            startPage();
        } else {
            PageUtils.clearPage();
        }
        List<DeviceDetails> list = deviceDetailsService.selectDeviceDetailsList(deviceDetails);
        if (!StringUtils.isEmpty(deviceDetails.getQueryType())) {
            int size = list.size();
            startPage();
            PageDomain pageDomain = TableSupport.buildPageRequest();
            Integer pageSize = pageDomain.getPageSize();
            Integer pageNum = pageDomain.getPageNum();
            int sl = pageSize * pageNum;
            if (list.size() < sl) {
                sl = list.size();
            }
            int ks = 0;
            if (pageNum != 1 && pageNum != 0) {
                ks = (pageNum - 1) * pageSize;
                if (ks < 0) {
                    ks = 0;
                }
            }
            list = list.subList(ks, sl);
            TableDataInfo dataTable = getDataTable(list);
            dataTable.setTotal(size);
            return dataTable;
        } else {
            return getDataTable(list);
        }
    }

    /**
     * 初始化材料特征项列表
     */
    // @RequiresPermissions("archives:materialItem:list")
    @GetMapping("/initData")
    public AjaxResult initData(DeviceDetails deviceDetails) {
        if (StringUtils.isEmpty(deviceDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        deviceDetailsService.initData(deviceDetails);
        return toAjax(true);
    }

    /**
     * 导出设备详情列表
     */
    // @RequiresPermissions("archives:deviceDetails:export")
    @Log(title = "设备详情", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DeviceDetails deviceDetails) {
        List<DeviceDetails> list = deviceDetailsService.selectDeviceDetailsList(deviceDetails);
        ExcelUtil<DeviceDetails> util = new ExcelUtil<DeviceDetails>(DeviceDetails.class);
        util.exportExcel(response, list, "设备详情数据");
    }

    /**
     * 获取设备详情详细信息
     */
    // @RequiresPermissions("archives:deviceDetails:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(deviceDetailsService.selectDeviceDetailsById(id));
    }

    /**
     * 新增设备详情
     */
    // @RequiresPermissions("archives:deviceDetails:add")
    @Log(title = "设备详情", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody DeviceDetails deviceDetails) {
        return toAjax(deviceDetailsService.insertDeviceDetails(deviceDetails));
    }

    /**
     * 修改设备详情
     */
    // @RequiresPermissions("archives:deviceDetails:edit")
    @Log(title = "设备详情", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DeviceDetails deviceDetails) {
        return toAjax(deviceDetailsService.updateDeviceDetails(deviceDetails));
    }

    /**
     * 删除设备详情
     */
    // @RequiresPermissions("archives:deviceDetails:remove")
    @Log(title = "设备详情", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(deviceDetailsService.deleteDeviceDetailsByIds(ids));
    }

    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody DeviceDetails materialType) {
        int i = deviceDetailsService.addToMain(materialType);
        if (i == -1) {
            return AjaxResult.success("编码已存在");
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
    public AjaxResult associationToMain(@RequestBody DeviceDetails materialType) {
        return toAjax(deviceDetailsService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody DeviceDetails materialType) {
        return toAjax(deviceDetailsService.unAssociationToMain(materialType));
    }

}
