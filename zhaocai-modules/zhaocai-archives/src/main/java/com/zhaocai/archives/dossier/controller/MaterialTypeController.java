package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.service.IMaterialTypeService;
import com.zhaocai.archives.dossier.tree.MaterialTypeTree;
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
 * 材料分类Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/materialType")
public class MaterialTypeController extends BaseController {
    @Autowired
    private IMaterialTypeService materialTypeService;

    /**
     * 查询材料分类列表
     */
    // @RequiresPermissions("archives:materialType:list")
    @GetMapping("/list")
    public TableDataInfo list(MaterialType materialType) {
        startPage();
        List<MaterialType> list = materialTypeService.selectMaterialTypeList(materialType);
        return getDataTable(list);
    }


    /**
     * 初始化
     */
    // @RequiresPermissions("archives:materialType:list")
    @GetMapping("/initData")
    public AjaxResult initData(MaterialType materialType) {
        if (StringUtils.isEmpty(materialType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        materialTypeService.initData(materialType);
        return toAjax(true);
    }


    /**
     * 获取材料分类树列表
     *
     * @param materialType
     * @return
     */
    @GetMapping("/getMaterialTypeTree")
    public List<MaterialTypeTree> getMaterialTypeTree(MaterialType materialType) {
        return materialTypeService.getMaterialTypeTree(materialType);
    }


    /**
     * 获取项目树列表
     *
     * @return
     */
    @GetMapping("/getDeptTree")
    public List<MaterialTypeTree> getDeptTree() {
        return materialTypeService.getDeptTree();
    }


    @GetMapping("/getSecondaryUnit")
    public Map<String, String> getSecondaryUnit(String organCode) {
        return materialTypeService.getSecondaryUnit(organCode);
    }

    /**
     * 导出材料分类列表
     */
    // @RequiresPermissions("archives:materialType:export")
    @Log(title = "材料分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MaterialType materialType) {
        List<MaterialType> list = materialTypeService.selectMaterialTypeList(materialType);
        ExcelUtil<MaterialType> util = new ExcelUtil<MaterialType>(MaterialType.class);
        util.exportExcel(response, list, "材料分类数据");
    }

    /**
     * 获取材料分类详细信息
     */
    // @RequiresPermissions("archives:materialType:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(materialTypeService.selectMaterialTypeById(id));
    }


    /**
     * 新增材料类型生成编号
     */
    // @RequiresPermissions("archives:materialType:add")
    @GetMapping("/initMaterialType")
    public AjaxResult initMaterialType(MaterialType materialType) {
        return success(materialTypeService.initMaterialType(materialType));
    }


    /**
     * 新增材料分类
     */
    // @RequiresPermissions("archives:materialType:add")
    @Log(title = "材料分类", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MaterialType materialType) {
        return toAjax(materialTypeService.insertMaterialType(materialType));
    }

    /**
     * 修改材料分类
     */
    // @RequiresPermissions("archives:materialType:edit")
    @Log(title = "材料分类", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MaterialType materialType) {
        return toAjax(materialTypeService.updateMaterialType(materialType));
    }

    /**
     * 删除材料分类
     */
    // @RequiresPermissions("archives:materialType:remove")
    @Log(title = "材料分类", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(materialTypeService.deleteMaterialTypeByIds(ids));
    }

    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody MaterialType materialType) {
        return toAjax(materialTypeService.addToMain(materialType));
    }

    /**
     * 关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/associationToMain")
    public AjaxResult associationToMain(@RequestBody MaterialType materialType) {
        return toAjax(materialTypeService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody MaterialType materialType) {
        return toAjax(materialTypeService.unAssociationToMain(materialType));
    }

}
