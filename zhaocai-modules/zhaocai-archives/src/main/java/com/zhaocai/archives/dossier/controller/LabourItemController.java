package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.LabourItem;
import com.zhaocai.archives.dossier.service.ILabourItemService;
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
 * 劳务特征项Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/labourItem")
public class LabourItemController extends BaseController {
    @Autowired
    private ILabourItemService labourItemService;

    /**
     * 查询劳务特征项列表
     */
    // @RequiresPermissions("archives:labourItem:list")
    @GetMapping("/list")
    public TableDataInfo list(LabourItem labourItem) {
//        startPage();
        List<LabourItem> list = labourItemService.selectLabourItemList(labourItem);
        return getDataTable(list);
    }


    /**
     * 初始化材料特征项列表
     */
    // @RequiresPermissions("archives:materialItem:list")
    @GetMapping("/initData")
    public AjaxResult initData(LabourItem labourItem) {
        if (StringUtils.isEmpty(labourItem.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        labourItemService.initData(labourItem);
        return toAjax(true);
    }


    /**
     * 导出劳务特征项列表
     */
    // @RequiresPermissions("archives:labourItem:export")
    @Log(title = "劳务特征项", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LabourItem labourItem) {
        List<LabourItem> list = labourItemService.selectLabourItemList(labourItem);
        ExcelUtil<LabourItem> util = new ExcelUtil<LabourItem>(LabourItem.class);
        util.exportExcel(response, list, "劳务特征项数据");
    }

    /**
     * 获取劳务特征项详细信息
     */
    // @RequiresPermissions("archives:labourItem:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(labourItemService.selectLabourItemById(id));
    }

    /**
     * 新增劳务特征项
     */
    // @RequiresPermissions("archives:labourItem:add")
    @Log(title = "劳务特征项", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LabourItem labourItem) {
        return toAjax(labourItemService.insertLabourItem(labourItem));
    }

    /**
     * 修改劳务特征项
     */
    // @RequiresPermissions("archives:labourItem:edit")
    @Log(title = "劳务特征项", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LabourItem labourItem) {
        return toAjax(labourItemService.updateLabourItem(labourItem));
    }

    /**
     * 删除劳务特征项
     */
    // @RequiresPermissions("archives:labourItem:remove")
    @Log(title = "劳务特征项", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(labourItemService.deleteLabourItemByIds(ids));
    }


    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody LabourItem materialType) {
        int i = labourItemService.addToMain(materialType);
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
    public AjaxResult associationToMain(@RequestBody LabourItem materialType) {
        return toAjax(labourItemService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody LabourItem materialType) {
        return toAjax(labourItemService.unAssociationToMain(materialType));
    }

}
