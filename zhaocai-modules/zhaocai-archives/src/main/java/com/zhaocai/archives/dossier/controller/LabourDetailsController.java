package com.zhaocai.archives.dossier.controller;

import com.zhaocai.archives.dossier.domain.LabourDetails;
import com.zhaocai.archives.dossier.service.ILabourDetailsService;
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
 * 劳务详情Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/labourDetails")
public class LabourDetailsController extends BaseController {
    @Autowired
    private ILabourDetailsService labourDetailsService;

    /**
     * 查询劳务详情列表
     */
    // @RequiresPermissions("archives:labourDetails:list")
    @GetMapping("/list")
    public TableDataInfo list(LabourDetails labourDetails) {
        if (StringUtils.isEmpty(labourDetails.getQueryType())) {
            startPage();
        } else {
            PageUtils.clearPage();
        }
        List<LabourDetails> list = labourDetailsService.selectLabourDetailsList(labourDetails);
        if (!StringUtils.isEmpty(labourDetails.getQueryType())) {
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
    public AjaxResult initData(LabourDetails labourDetails) {
        if (StringUtils.isEmpty(labourDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        labourDetailsService.initData(labourDetails);
        return toAjax(true);
    }

    /**
     * 导出劳务详情列表
     */
    // @RequiresPermissions("archives:labourDetails:export")
    @Log(title = "劳务详情", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LabourDetails labourDetails) {
        List<LabourDetails> list = labourDetailsService.selectLabourDetailsList(labourDetails);
        ExcelUtil<LabourDetails> util = new ExcelUtil<LabourDetails>(LabourDetails.class);
        util.exportExcel(response, list, "劳务详情数据");
    }

    /**
     * 获取劳务详情详细信息
     */
    // @RequiresPermissions("archives:labourDetails:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        LabourDetails labourDetails = labourDetailsService.selectLabourDetailsById(id);
        if (labourDetails != null && "N".equals(labourDetails.getIsMain())) {
            labourDetails.setLabourCode(labourDetails.getLabourCode() + "-" + labourDetails.getOrganCode().substring(0, 4));
        }
        return success(labourDetails);
    }

    /**
     * 新增劳务详情
     */
    // @RequiresPermissions("archives:labourDetails:add")
    @Log(title = "劳务详情", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LabourDetails labourDetails) {
        return toAjax(labourDetailsService.insertLabourDetails(labourDetails));
    }

    /**
     * 修改劳务详情
     */
    // @RequiresPermissions("archives:labourDetails:edit")
    @Log(title = "劳务详情", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LabourDetails labourDetails) {
        return toAjax(labourDetailsService.updateLabourDetails(labourDetails));
    }

    /**
     * 删除劳务详情
     */
    // @RequiresPermissions("archives:labourDetails:remove")
    @Log(title = "劳务详情", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(labourDetailsService.deleteLabourDetailsByIds(ids));
    }

    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/addToMain")
    public AjaxResult addToMain(@RequestBody LabourDetails materialType) {
        return toAjax(labourDetailsService.addToMain(materialType));
    }

    /**
     * 关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/associationToMain")
    public AjaxResult associationToMain(@RequestBody LabourDetails materialType) {
        return toAjax(labourDetailsService.associationToMain(materialType));
    }

    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @PostMapping("/unAssociationToMain")
    public AjaxResult unAssociationToMain(@RequestBody LabourDetails materialType) {
        return toAjax(labourDetailsService.unAssociationToMain(materialType));
    }


    /**
     * 新增具体档案生成编号
     */
    // @RequiresPermissions("archives:materialType:add")
    @GetMapping("/initDetails")
    public AjaxResult initDetails(LabourDetails labourDetails) {
        return success(labourDetailsService.initDetails(labourDetails));
    }


}
