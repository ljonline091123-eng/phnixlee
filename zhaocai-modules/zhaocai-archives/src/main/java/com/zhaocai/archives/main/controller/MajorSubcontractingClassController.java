package com.zhaocai.archives.main.controller;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import com.zhaocai.archives.dossier.tree.LabourTypeTree;
import com.zhaocai.archives.dossier.tree.SubcontractingTypeTree;
import com.zhaocai.archives.main.domain.DeviceClass;
import com.zhaocai.archives.main.domain.MajorSubcontractingClassExcelData;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import com.zhaocai.common.security.annotation.RequiresPermissions;
import com.zhaocai.archives.main.domain.MajorSubcontractingClass;
import com.zhaocai.archives.main.service.IMajorSubcontractingClassService;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 专业分包分类主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/majorClass")
public class MajorSubcontractingClassController extends BaseController
{
    @Autowired
    private IMajorSubcontractingClassService majorSubcontractingClassService;

    /**
     * 查询专业分包分类主列表
     */
    // @RequiresPermissions("archives:majorClass:list")
    @GetMapping("/list")
    public TableDataInfo list(MajorSubcontractingClass majorSubcontractingClass)
    {
        startPage();
        List<MajorSubcontractingClass> list = majorSubcontractingClassService.selectMajorSubcontractingClassList(majorSubcontractingClass);
        return getDataTable(list);
    }

    /**
     * 导出专业分包分类主列表
     */
    // @RequiresPermissions("archives:majorClass:export")
    @Log(title = "专业分包分类主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MajorSubcontractingClass majorSubcontractingClass)
    {
        List<MajorSubcontractingClass> list = majorSubcontractingClassService.selectMajorSubcontractingClassList(majorSubcontractingClass);
        ExcelUtil<MajorSubcontractingClass> util = new ExcelUtil<MajorSubcontractingClass>(MajorSubcontractingClass.class);
        util.exportExcel(response, list, "专业分包分类主数据");
    }

    /**
     * 获取专业分包分类主详细信息
     */
    // @RequiresPermissions("archives:majorClass:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(majorSubcontractingClassService.selectMajorSubcontractingClassById(id));
    }

    /**
     * 新增专业分包分类主
     */
    // @RequiresPermissions("archives:majorClass:add")
    @Log(title = "专业分包分类主", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MajorSubcontractingClass majorSubcontractingClass)
    {
        return toAjax(majorSubcontractingClassService.insertMajorSubcontractingClass(majorSubcontractingClass));
    }

    /**
     * 修改专业分包分类主
     */
    // @RequiresPermissions("archives:majorClass:edit")
    @Log(title = "专业分包分类主", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MajorSubcontractingClass majorSubcontractingClass)
    {
        return toAjax(majorSubcontractingClassService.updateMajorSubcontractingClass(majorSubcontractingClass));
    }

    /**
     * 删除专业分包分类主
     */
    // @RequiresPermissions("archives:majorClass:remove")
    @Log(title = "专业分包分类主", businessType = BusinessType.DELETE)
	@PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(majorSubcontractingClassService.deleteMajorSubcontractingClassByIds(ids));
    }


    /**
     * 树
     *
     * @return
     */
    @GetMapping("/getMajorSubcontractingClassTree")
    public List<SubcontractingTypeTree> getMajorSubcontractingClassTree() {
        return majorSubcontractingClassService.getMajorSubcontractingClassTree();
    }

    @GetMapping(value = "/initCode")
    public AjaxResult initCode(MajorSubcontractingClass aClass) {
        return success(majorSubcontractingClassService.initCode(aClass));
    }



    @Log(title = "专业分包导入", businessType = BusinessType.IMPORT)
//    @RequiresPermissions("system:user:import")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<MajorSubcontractingClassExcelData> util = new ExcelUtil<MajorSubcontractingClassExcelData>(MajorSubcontractingClassExcelData.class);
        List<MajorSubcontractingClassExcelData> userList = util.importExcel(file.getInputStream());
        String operName = SecurityUtils.getUsername();
        String message = majorSubcontractingClassService.importData(userList, updateSupport, operName);
        return success(message);
    }


    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<MajorSubcontractingClassExcelData> util = new ExcelUtil<MajorSubcontractingClassExcelData>(MajorSubcontractingClassExcelData.class);
        util.importTemplateExcel(response, "专业分包导入");
    }


}
