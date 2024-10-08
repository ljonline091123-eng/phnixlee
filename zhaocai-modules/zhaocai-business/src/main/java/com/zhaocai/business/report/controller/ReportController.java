package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.*;
import com.zhaocai.business.report.vo.*;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.core.web.domain.AjaxResult;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report")
@Api(value = "报表列表对象", tags = "报表列表对象")
public class ReportController extends BladeController {

    @Autowired
    private ITenderingRateReportService tenderingRateReportService;

    @Autowired
    private IContractLedgerReportService contractLedgerReportService;

    @Autowired
    private IPriceAnalysisReportService priceAnalysisReportService;

    @Autowired
    private IManagePageReportService managePageReportService;

    @Autowired
    private IVBidCountService bidCountService;

    /**
     * 招标率报表
     * @param tenderingRate
     * @return
     */
    @GetMapping("/tenderingRateReport")
    public ResultData<List<TenderingRateReportVo>> tenderingRateReport(TenderingRateReportVo tenderingRate){
        return ResultData.data(tenderingRateReportService.tenderingRateReport(tenderingRate));
    }

    /**
     * 招标率报表
     * @param vBidCountVo
     * @return
     */
    @GetMapping("/bidCountReport")
    public ResultData<List<VBidCountVo>> bidCountReport(VBidCountVo vBidCountVo){
        return ResultData.data(bidCountService.bidCountReport(vBidCountVo));
    }

    /**
     * 招标率报表(懒加载)
     * @param vBidCountVo
     * @return
     */
    @GetMapping("/bidCountReportLazy")
    public ResultData<List<VBidCountVo>> bidCountReportLazy(VBidCountVo vBidCountVo){
        return ResultData.data(bidCountService.bidCountReportLazy(vBidCountVo));
    }

    /**
     * 招标率报表导出
     * @param vBidCountVo
     * @return
     */
    @PostMapping("/bidCountReportExport")
    public void bidCountReportExport(HttpServletResponse response,VBidCountVo vBidCountVo) {
        List<VBidCountVo> list = bidCountService.bidCountReportExport(vBidCountVo);
        String title = bidCountService.getExportTitle(vBidCountVo);
        ExcelUtil<VBidCountVo> util = new ExcelUtil<VBidCountVo>(VBidCountVo.class);
        util.exportExcel(response,list, "招标率报表数据",title + "招标率统计报表");
    }

    /**
     * 获取组织及以下所有项目编码
     * @param id
     * @return
     */
    @GetMapping("/getBidCountProjectCode")
    public AjaxResult getBidCountProjectCode(String id){
        return AjaxResult.success(bidCountService.getBidCountProjectCode(id));
    }

    /**
     * 合同台账报表
     * @param contractLedger
     * @return
     */
    @GetMapping("/contractLedgerReport")
    public ResultData<List<ContractLedgerReportVo>> contractLedgerReport(ContractLedgerReportVo contractLedger){
        return ResultData.data(contractLedgerReportService.contractLedgerReport(contractLedger));
    }

    /**
     * 价格分析报表
     * @param priceAnalysis
     * @return
     */
    @GetMapping("/priceAnalysisReport")
    public ResultData<List<PriceAnalysisReportVo>> priceAnalysisReport(PriceAnalysisReportVo priceAnalysis){
        return ResultData.data(priceAnalysisReportService.priceAnalysisReport(priceAnalysis));
    }

    /**
     * 公司/集团管理界面
     * @param managePage
     * @return
     */
    @GetMapping("/managePageReport")
    public AjaxResult managePageReport(ManagePageReportVo managePage){
        return AjaxResult.success(managePageReportService.managePageReport(managePage));
    }

    /**
     * 公司/集团管理导出
     * @param managePage
     * @return
     */
    @PostMapping("/managePageReportExport")
    public AjaxResult export(HttpServletResponse response,ManagePageReportVo managePage) {
        Map<String, Object> map = managePageReportService.managePageReport(managePage);
        List<ManagePageReportVo> list = (List<ManagePageReportVo>) map.get("list");
        ExcelUtil<ManagePageReportVo> util = new ExcelUtil<ManagePageReportVo>(ManagePageReportVo.class);
        return util.exportExcel(response,list, "项目数据");
    }

    /**
     * 公司/集团管理组织查询
     * @param managePage
     * @return
     */
    @GetMapping("/managePageReportDept")
    public AjaxResult managePageReportDept(ManagePageReportVo managePage) {
        return AjaxResult.success(managePageReportService.managePageReportDept(managePage));
    }
}
