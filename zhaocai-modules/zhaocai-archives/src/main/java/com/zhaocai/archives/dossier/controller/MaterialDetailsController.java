package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.MaterialDetails;
import com.zhaocai.archives.dossier.service.IMaterialDetailsService;
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
 * 材料详情Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/materialDetails")
public class MaterialDetailsController extends BaseController {
    @Autowired
    private IMaterialDetailsService materialDetailsService;

    /**
     * 查询材料详情列表
     */
    // @RequiresPermissions("archives:materialDetails:list")
    @GetMapping("/list")
    public TableDataInfo list(MaterialDetails materialDetails) {
        if (StringUtils.isEmpty(materialDetails.getQueryType())) {
            startPage();
        } else {
            PageUtils.clearPage();
        }
        List<MaterialDetails> list = materialDetailsService.selectMaterialDetailsList(materialDetails);
        if (!StringUtils.isEmpty(materialDetails.getQueryType())) {
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
    public AjaxResult initData(MaterialDetails materialDetails) {
        if (StringUtils.isEmpty(materialDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        materialDetailsService.initData(materialDetails);
        return toAjax(true);
    }


    /**
     * 导出材料详情列表
     */
    // @RequiresPermissions("archives:materialDetails:export")
    @Log(title = "材料详情", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MaterialDetails materialDetails) {
        List<MaterialDetails> list = materialDetailsService.selectMaterialDetailsList(materialDetails);
        ExcelUtil<MaterialDetails> util = new ExcelUtil<MaterialDetails>(MaterialDetails.class);
        util.exportExcel(response, list, "材料详情数据");
    }

    /**
     * 获取材料详情详细信息
     */
    // @RequiresPermissions("archives:materialDetails:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(materialDetailsService.selectMaterialDetailsById(id));
    }

    /**
     * 新增材料详情
     */
    // @RequiresPermissions("archives:materialDetails:add")
    @Log(title = "材料详情", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MaterialDetails materialDetails) {
        return toAjax(materialDetailsService.insertMaterialDetails(materialDetails));
    }

    /**
     * 修改材料详情
     */
    // @RequiresPermissions("archives:materialDetails:edit")
    @Log(title = "材料详情", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MaterialDetails materialDetails) {
        return toAjax(materialDetailsService.updateMaterialDetails(materialDetails));
    }

    /**
     * 删除材料详情
     */
    // @RequiresPermissions("archives:materialDetails:remove")
    @Log(title = "材料详情", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(materialDetailsService.deleteMaterialDetailsByIds(ids));
    }

    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody MaterialDetails materialType) {
        int i = materialDetailsService.addToMain(materialType);
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
    public AjaxResult associationToMain(@RequestBody MaterialDetails materialType) {
        return toAjax(materialDetailsService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody MaterialDetails materialType) {
        return toAjax(materialDetailsService.unAssociationToMain(materialType));
    }


}
