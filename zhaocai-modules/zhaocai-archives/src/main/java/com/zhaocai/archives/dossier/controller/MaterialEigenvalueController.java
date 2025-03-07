package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.MaterialEigenvalue;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.service.IMaterialEigenvalueService;
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
 * 材料特征值Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/materialEigenvalue")
public class MaterialEigenvalueController extends BaseController {
    @Autowired
    private IMaterialEigenvalueService materialEigenvalueService;

    /**
     * 查询材料特征值列表
     */
    // @RequiresPermissions("archives:materialEigenvalue:list")
    @GetMapping("/list")
    public TableDataInfo list(MaterialEigenvalue materialEigenvalue) {
//        startPage();
        List<MaterialEigenvalue> list = materialEigenvalueService.selectMaterialEigenvalueList(materialEigenvalue);
        return getDataTable(list);
    }


    /**
     * 初始化材料特征项列表
     */
    // @RequiresPermissions("archives:materialItem:list")
    @GetMapping("/initData")
    public AjaxResult initData(MaterialEigenvalue materialEigenvalue) {
        if (StringUtils.isEmpty(materialEigenvalue.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        materialEigenvalueService.initData(materialEigenvalue);
        return toAjax(true);
    }


    /**
     * 导出材料特征值列表
     */
    // @RequiresPermissions("archives:materialEigenvalue:export")
    @Log(title = "材料特征值", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MaterialEigenvalue materialEigenvalue) {
        List<MaterialEigenvalue> list = materialEigenvalueService.selectMaterialEigenvalueList(materialEigenvalue);
        ExcelUtil<MaterialEigenvalue> util = new ExcelUtil<MaterialEigenvalue>(MaterialEigenvalue.class);
        util.exportExcel(response, list, "材料特征值数据");
    }

    /**
     * 获取材料特征值详细信息
     */
    // @RequiresPermissions("archives:materialEigenvalue:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(materialEigenvalueService.selectMaterialEigenvalueById(id));
    }

    /**
     * 新增材料特征值
     */
    // @RequiresPermissions("archives:materialEigenvalue:add")
    @Log(title = "材料特征值", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MaterialEigenvalue materialEigenvalue) {
        return toAjax(materialEigenvalueService.insertMaterialEigenvalue(materialEigenvalue));
    }

    /**
     * 修改材料特征值
     */
    // @RequiresPermissions("archives:materialEigenvalue:edit")
    @Log(title = "材料特征值", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MaterialEigenvalue materialEigenvalue) {
        return toAjax(materialEigenvalueService.updateMaterialEigenvalue(materialEigenvalue));
    }

    /**
     * 删除材料特征值
     */
    // @RequiresPermissions("archives:materialEigenvalue:remove")
    @Log(title = "材料特征值", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(materialEigenvalueService.deleteMaterialEigenvalueByIds(ids));
    }

    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody MaterialEigenvalue materialType) {
        int i = materialEigenvalueService.addToMain(materialType);
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
    public AjaxResult associationToMain(@RequestBody MaterialEigenvalue materialType) {
        return toAjax(materialEigenvalueService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody MaterialEigenvalue materialType) {
        return toAjax(materialEigenvalueService.unAssociationToMain(materialType));
    }

}
