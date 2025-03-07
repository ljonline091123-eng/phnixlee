package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.SubcontractingDetails;
import com.zhaocai.archives.dossier.service.ISubcontractingDetailsService;
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
 * 专业分包详情Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/subcontractingDetails")
public class SubcontractingDetailsController extends BaseController {
    @Autowired
    private ISubcontractingDetailsService subcontractingDetailsService;

    /**
     * 查询专业分包详情列表
     */
    // @RequiresPermissions("archives:subcontractingDetails:list")
    @GetMapping("/list")
    public TableDataInfo list(SubcontractingDetails subcontractingDetails) {
        if (StringUtils.isEmpty(subcontractingDetails.getQueryType())) {
            startPage();
        } else {
            PageUtils.clearPage();
        }
        List<SubcontractingDetails> list = subcontractingDetailsService.selectSubcontractingDetailsList(subcontractingDetails);
        if (!StringUtils.isEmpty(subcontractingDetails.getQueryType())) {
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
    public AjaxResult initData(SubcontractingDetails subcontractingDetails) {
        if (StringUtils.isEmpty(subcontractingDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        subcontractingDetailsService.initData(subcontractingDetails);
        return toAjax(true);
    }

    /**
     * 导出专业分包详情列表
     */
    // @RequiresPermissions("archives:subcontractingDetails:export")
    @Log(title = "专业分包详情", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SubcontractingDetails subcontractingDetails) {
        List<SubcontractingDetails> list = subcontractingDetailsService.selectSubcontractingDetailsList(subcontractingDetails);
        ExcelUtil<SubcontractingDetails> util = new ExcelUtil<SubcontractingDetails>(SubcontractingDetails.class);
        util.exportExcel(response, list, "专业分包详情数据");
    }

    /**
     * 获取专业分包详情详细信息
     */
    // @RequiresPermissions("archives:subcontractingDetails:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        SubcontractingDetails subcontractingDetails = subcontractingDetailsService.selectSubcontractingDetailsById(id);
        if (subcontractingDetails != null && "N".equals(subcontractingDetails.getIsMain())) {
            subcontractingDetails.setSubcontractingCode(subcontractingDetails.getSubcontractingCode() + "-" + subcontractingDetails.getOrganCode().substring(0, 4));
        }
        return success(subcontractingDetails);
    }

    /**
     * 新增专业分包详情
     */
    // @RequiresPermissions("archives:subcontractingDetails:add")
    @Log(title = "专业分包详情", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody SubcontractingDetails subcontractingDetails) {
        return toAjax(subcontractingDetailsService.insertSubcontractingDetails(subcontractingDetails));
    }

    /**
     * 修改专业分包详情
     */
    // @RequiresPermissions("archives:subcontractingDetails:edit")
    @Log(title = "专业分包详情", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody SubcontractingDetails subcontractingDetails) {
        return toAjax(subcontractingDetailsService.updateSubcontractingDetails(subcontractingDetails));
    }

    /**
     * 删除专业分包详情
     */
    // @RequiresPermissions("archives:subcontractingDetails:remove")
    @Log(title = "专业分包详情", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(subcontractingDetailsService.deleteSubcontractingDetailsByIds(ids));
    }


    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody SubcontractingDetails materialType) {
        return toAjax(subcontractingDetailsService.addToMain(materialType));
    }

    /**
     * 关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/associationToMain")
    public AjaxResult associationToMain(@RequestBody SubcontractingDetails materialType) {
        return toAjax(subcontractingDetailsService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody SubcontractingDetails materialType) {
        return toAjax(subcontractingDetailsService.unAssociationToMain(materialType));
    }


    /**
     * 新增具体档案生成编号
     */
    // @RequiresPermissions("archives:materialType:add")
    @GetMapping("/initDetails")
    public AjaxResult initDetails(SubcontractingDetails subcontractingDetails) {
        return success(subcontractingDetailsService.initDetails(subcontractingDetails));
    }

}
