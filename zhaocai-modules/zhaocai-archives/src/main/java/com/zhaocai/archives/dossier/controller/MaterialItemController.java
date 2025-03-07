package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.MaterialItem;
import com.zhaocai.archives.dossier.service.IMaterialItemService;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.web.page.TableDataInfo;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 材料特征项Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/materialItem")
public class MaterialItemController extends BaseController {
    @Autowired
    private IMaterialItemService materialItemService;

    /**
     * 查询材料特征项列表
     */
    // @RequiresPermissions("archives:materialItem:list")
    @GetMapping("/list")
    public TableDataInfo list(MaterialItem materialItem) {
//        startPage();
        List<MaterialItem> list = materialItemService.selectMaterialItemList(materialItem);
        return getDataTable(list);
    }


    /**
     * 初始化材料特征项列表
     */
    // @RequiresPermissions("archives:materialItem:list")
    @GetMapping("/initData")
    public AjaxResult initData(MaterialItem materialItem) {
        if (StringUtils.isEmpty(materialItem.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        materialItemService.initData(materialItem);
        return toAjax(true);
    }


    /**
     * 获取材料特征项详细信息
     */
    // @RequiresPermissions("archives:materialItem:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(materialItemService.selectMaterialItemById(id));
    }

    /**
     * 新增材料特征项
     */
    // @RequiresPermissions("archives:materialItem:add")
    @Log(title = "材料特征项", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MaterialItem materialItem) {
        return toAjax(materialItemService.insertMaterialItem(materialItem));
    }

    /**
     * 修改材料特征项
     */
    // @RequiresPermissions("archives:materialItem:edit")
    @Log(title = "材料特征项", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MaterialItem materialItem) {
        return toAjax(materialItemService.updateMaterialItem(materialItem));
    }

    /**
     * 删除材料特征项
     */
    // @RequiresPermissions("archives:materialItem:remove")
    @Log(title = "材料特征项", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(materialItemService.deleteMaterialItemByIds(ids));
    }

    /**
     * 新增至主库
     *
     * @param materialItem
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody MaterialItem materialItem) {
        int i = materialItemService.addToMain(materialItem);
        if (i == -1) {
            return AjaxResult.success("编码已存在");
        }
        return toAjax(i);
    }

    /**
     * 关联至主库
     *
     * @param materialItem
     * @return
     */
    @PostMapping("/associationToMain")
    public AjaxResult associationToMain(@RequestBody MaterialItem materialItem) {
        return toAjax(materialItemService.associationToMain(materialItem));
    }

    /**
     * 取消关联至主库
     *
     * @param materialItem
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody MaterialItem materialItem) {
        return toAjax(materialItemService.unAssociationToMain(materialItem));
    }


}
