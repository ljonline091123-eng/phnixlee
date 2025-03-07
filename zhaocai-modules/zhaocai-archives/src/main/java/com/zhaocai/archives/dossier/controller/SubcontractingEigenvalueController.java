package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.SubcontractingEigenvalue;
import com.zhaocai.archives.dossier.domain.SubcontractingItem;
import com.zhaocai.archives.dossier.service.ISubcontractingEigenvalueService;
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
 * 专业分包特征值Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/subcontractingEigenvalue")
public class SubcontractingEigenvalueController extends BaseController {
    @Autowired
    private ISubcontractingEigenvalueService subcontractingEigenvalueService;

    /**
     * 查询专业分包特征值列表
     */
    // @RequiresPermissions("archives:subcontractingEigenvalue:list")
    @GetMapping("/list")
    public TableDataInfo list(SubcontractingEigenvalue subcontractingEigenvalue) {
//        startPage();
        List<SubcontractingEigenvalue> list = subcontractingEigenvalueService.selectSubcontractingEigenvalueList(subcontractingEigenvalue);
        return getDataTable(list);
    }


    /**
     * 初始化材料特征项列表
     */
    // @RequiresPermissions("archives:materialItem:list")
    @GetMapping("/initData")
    public AjaxResult initData(SubcontractingEigenvalue subcontractingEigenvalue) {
        if (StringUtils.isEmpty(subcontractingEigenvalue.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        subcontractingEigenvalueService.initData(subcontractingEigenvalue);
        return toAjax(true);
    }


    /**
     * 导出专业分包特征值列表
     */
    // @RequiresPermissions("archives:subcontractingEigenvalue:export")
    @Log(title = "专业分包特征值", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SubcontractingEigenvalue subcontractingEigenvalue) {
        List<SubcontractingEigenvalue> list = subcontractingEigenvalueService.selectSubcontractingEigenvalueList(subcontractingEigenvalue);
        ExcelUtil<SubcontractingEigenvalue> util = new ExcelUtil<SubcontractingEigenvalue>(SubcontractingEigenvalue.class);
        util.exportExcel(response, list, "专业分包特征值数据");
    }

    /**
     * 获取专业分包特征值详细信息
     */
    // @RequiresPermissions("archives:subcontractingEigenvalue:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(subcontractingEigenvalueService.selectSubcontractingEigenvalueById(id));
    }

    /**
     * 新增专业分包特征值
     */
    // @RequiresPermissions("archives:subcontractingEigenvalue:add")
    @Log(title = "专业分包特征值", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody SubcontractingEigenvalue subcontractingEigenvalue) {
        return toAjax(subcontractingEigenvalueService.insertSubcontractingEigenvalue(subcontractingEigenvalue));
    }

    /**
     * 修改专业分包特征值
     */
    // @RequiresPermissions("archives:subcontractingEigenvalue:edit")
    @Log(title = "专业分包特征值", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody SubcontractingEigenvalue subcontractingEigenvalue) {
        return toAjax(subcontractingEigenvalueService.updateSubcontractingEigenvalue(subcontractingEigenvalue));
    }

    /**
     * 删除专业分包特征值
     */
    // @RequiresPermissions("archives:subcontractingEigenvalue:remove")
    @Log(title = "专业分包特征值", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(subcontractingEigenvalueService.deleteSubcontractingEigenvalueByIds(ids));
    }


    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody SubcontractingEigenvalue materialType) {
        int i = subcontractingEigenvalueService.addToMain(materialType);
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
    public AjaxResult associationToMain(@RequestBody SubcontractingEigenvalue materialType) {
        return toAjax(subcontractingEigenvalueService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody SubcontractingEigenvalue materialType) {
        return toAjax(subcontractingEigenvalueService.unAssociationToMain(materialType));
    }

}
