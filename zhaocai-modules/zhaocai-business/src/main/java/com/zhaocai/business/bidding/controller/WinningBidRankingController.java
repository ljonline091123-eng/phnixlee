package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.domain.WinningBidRanking;
import com.zhaocai.business.bidding.service.IWinningBidRankingService;
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
 * 中标排行榜Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/ranking")
public class WinningBidRankingController extends BaseController
{
    @Autowired
    private IWinningBidRankingService winningBidRankingService;

    /**
     * 查询中标排行榜列表
     */
    @GetMapping("/list")
    public TableDataInfo list(WinningBidRanking winningBidRanking)
    {
        startPage();
        List<WinningBidRanking> list = winningBidRankingService.selectWinningBidRankingList(winningBidRanking);
        return getDataTable(list);
    }

    /**
     * 导出中标排行榜列表
     */
    @Log(title = "中标排行榜", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WinningBidRanking winningBidRanking)
    {
        List<WinningBidRanking> list = winningBidRankingService.selectWinningBidRankingList(winningBidRanking);
        ExcelUtil<WinningBidRanking> util = new ExcelUtil<WinningBidRanking>(WinningBidRanking.class);
        util.exportExcel(response, list, "中标排行榜数据");
    }

    /**
     * 获取中标排行榜详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(winningBidRankingService.selectWinningBidRankingById(id));
    }

}
