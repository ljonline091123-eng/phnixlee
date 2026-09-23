package com.zhaocai.business.expert.controller;

import com.zhaocai.business.expert.domain.ExpertScoreDetail;
import com.zhaocai.business.expert.service.IExpertScoreDetailService;
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
 * 专家评分明细Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/detail")
public class ExpertScoreDetailController extends BaseController
{
    @Autowired
    private IExpertScoreDetailService expertScoreDetailService;

    /**
     * 查询专家评分明细列表
     */
    @GetMapping("/list")
    public TableDataInfo list(ExpertScoreDetail expertScoreDetail)
    {
        startPage();
        List<ExpertScoreDetail> list = expertScoreDetailService.selectExpertScoreDetailList(expertScoreDetail);
        return getDataTable(list);
    }

    /**
     * 导出专家评分明细列表
     */
    @Log(title = "专家评分明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ExpertScoreDetail expertScoreDetail)
    {
        List<ExpertScoreDetail> list = expertScoreDetailService.selectExpertScoreDetailList(expertScoreDetail);
        ExcelUtil<ExpertScoreDetail> util = new ExcelUtil<ExpertScoreDetail>(ExpertScoreDetail.class);
        util.exportExcel(response, list, "专家评分明细数据");
    }

    /**
     * 获取专家评分明细详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(expertScoreDetailService.selectExpertScoreDetailById(id));
    }

    /**
     * 新增专家评分明细
     */
    @Log(title = "专家评分明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ExpertScoreDetail expertScoreDetail)
    {
        return toAjax(expertScoreDetailService.insertExpertScoreDetail(expertScoreDetail));
    }

    /**
     * 修改专家评分明细
     */
    @Log(title = "专家评分明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ExpertScoreDetail expertScoreDetail)
    {
        return toAjax(expertScoreDetailService.updateExpertScoreDetail(expertScoreDetail));
    }

    /**
     * 删除专家评分明细
     */
    @Log(title = "专家评分明细", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(expertScoreDetailService.deleteExpertScoreDetailByIds(ids));
    }
}
