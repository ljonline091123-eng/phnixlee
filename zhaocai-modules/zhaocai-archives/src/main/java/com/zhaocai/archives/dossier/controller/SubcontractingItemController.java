package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.SubcontractingItem;
import com.zhaocai.archives.dossier.service.ISubcontractingItemService;
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
 * 专业分包特征项Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/subcontractingItem")
public class SubcontractingItemController extends BaseController {
    @Autowired
    private ISubcontractingItemService subcontractingItemService;

    /**
     * 查询专业分包特征项列表
     */
    // @RequiresPermissions("archives:subcontractingItem:list")
    @GetMapping("/list")
    public TableDataInfo list(SubcontractingItem subcontractingItem) {
//        startPage();
        List<SubcontractingItem> list = subcontractingItemService.selectSubcontractingItemList(subcontractingItem);
        return getDataTable(list);
    }


    /**
     * 初始化材料特征项列表
     */
    // @RequiresPermissions("archives:materialItem:list")
    @GetMapping("/initData")
    public AjaxResult initData(SubcontractingItem subcontractingItem) {
        if (StringUtils.isEmpty(subcontractingItem.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        subcontractingItemService.initData(subcontractingItem);
        return toAjax(true);
    }

    /**
     * 导出专业分包特征项列表
     */
    // @RequiresPermissions("archives:subcontractingItem:export")
    @Log(title = "专业分包特征项", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SubcontractingItem subcontractingItem) {
        List<SubcontractingItem> list = subcontractingItemService.selectSubcontractingItemList(subcontractingItem);
        ExcelUtil<SubcontractingItem> util = new ExcelUtil<SubcontractingItem>(SubcontractingItem.class);
        util.exportExcel(response, list, "专业分包特征项数据");
    }

    /**
     * 获取专业分包特征项详细信息
     */
    // @RequiresPermissions("archives:subcontractingItem:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(subcontractingItemService.selectSubcontractingItemById(id));
    }

    /**
     * 新增专业分包特征项
     */
    // @RequiresPermissions("archives:subcontractingItem:add")
    @Log(title = "专业分包特征项", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody SubcontractingItem subcontractingItem) {
        return toAjax(subcontractingItemService.insertSubcontractingItem(subcontractingItem));
    }

    /**
     * 修改专业分包特征项
     */
    // @RequiresPermissions("archives:subcontractingItem:edit")
    @Log(title = "专业分包特征项", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody SubcontractingItem subcontractingItem) {
        return toAjax(subcontractingItemService.updateSubcontractingItem(subcontractingItem));
    }

    /**
     * 删除专业分包特征项
     */
    // @RequiresPermissions("archives:subcontractingItem:remove")
    @Log(title = "专业分包特征项", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(subcontractingItemService.deleteSubcontractingItemByIds(ids));
    }


    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody SubcontractingItem materialType) {
        int i = subcontractingItemService.addToMain(materialType);
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
    public AjaxResult associationToMain(@RequestBody SubcontractingItem materialType) {
        return toAjax(subcontractingItemService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody SubcontractingItem materialType) {
        return toAjax(subcontractingItemService.unAssociationToMain(materialType));
    }

}
