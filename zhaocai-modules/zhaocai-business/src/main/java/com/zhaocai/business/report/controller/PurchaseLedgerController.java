package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.IPurchaseLedgerService;
import com.zhaocai.business.report.vo.req.PurchaseLedgerQueryVo;
import com.zhaocai.business.report.vo.res.PurchaseLedgerListVo;
import com.zhaocai.business.report.vo.res.PurchaseLedgerSummaryVo;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.core.web.domain.AjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 采购台账报表
 *
 * 查询/导出/刷数全部独立于此 Controller，不修改 ReportController 等已有文件。
 * 数据权限由 Service 内 ReportScopeUtil 强制收敛。
 *
 * @author claude
 */
@RestController
@RequestMapping("/report")
@Api(value = "采购台账报表")
public class PurchaseLedgerController extends BladeController {

    @Autowired
    private IPurchaseLedgerService purchaseLedgerService;

    /**
     * 汇总视图聚合查询（组织 × 项目 × 采购需求类型，五类状态数字）
     */
    @ApiOperation("采购台账-汇总")
    @GetMapping("/purchaseLedgerSummary")
    public AjaxResult purchaseLedgerSummary(PurchaseLedgerQueryVo queryVo) {
        return AjaxResult.success(purchaseLedgerService.getSummary(queryVo));
    }

    /**
     * 明细宽表查询（分页）
     */
    @ApiOperation("采购台账-明细")
    @GetMapping("/purchaseLedgerList")
    public AjaxResult purchaseLedgerList(PurchaseLedgerQueryVo queryVo) {
        return AjaxResult.success(purchaseLedgerService.getLedgerList(queryVo));
    }

    /**
     * 汇总视图导出
     */
    @ApiOperation("采购台账-汇总导出")
    @PostMapping("/purchaseLedgerSummaryExport")
    public void purchaseLedgerSummaryExport(HttpServletResponse response, PurchaseLedgerQueryVo queryVo) {
        Map<String, Object> map = purchaseLedgerService.summaryExport(queryVo);
        ExcelUtil<PurchaseLedgerSummaryVo> util = new ExcelUtil<PurchaseLedgerSummaryVo>(PurchaseLedgerSummaryVo.class);
        util.exportExcel(response, (List<PurchaseLedgerSummaryVo>) map.get("list"),
                "采购台账汇总数据", map.get("title") + "汇总报表");
    }

    /**
     * 明细宽表导出（完整结果，不受分页限制）
     */
    @ApiOperation("采购台账-导出")
    @PostMapping("/purchaseLedgerExport")
    public void purchaseLedgerExport(HttpServletResponse response, PurchaseLedgerQueryVo queryVo) {
        Map<String, Object> map = purchaseLedgerService.ledgerExport(queryVo);
        ExcelUtil<PurchaseLedgerListVo> util = new ExcelUtil<PurchaseLedgerListVo>(PurchaseLedgerListVo.class);
        util.exportExcel(response, (List<PurchaseLedgerListVo>) map.get("list"),
                "采购台账明细数据", map.get("title") + "明细报表");
    }

    /**
     * 定时刷新采购台账物化表（sys_job 通过 Feign 调用，带 INNER 来源头）
     */
    @ApiOperation("定时刷新采购台账报表数据")
    @GetMapping("/purchaseLedgerHandleReport")
    public ResultData<Boolean> purchaseLedgerHandleReport() {
        return ResultData.status(purchaseLedgerService.handlePurchaseLedgerReport());
    }
}
