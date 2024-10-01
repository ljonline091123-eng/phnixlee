package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.IContractLedgerReportService;
import com.zhaocai.business.report.service.IManagePageReportService;
import com.zhaocai.business.report.service.IPriceAnalysisReportService;
import com.zhaocai.business.report.service.ITenderingRateReportService;
import com.zhaocai.business.report.vo.ContractLedgerReportVo;
import com.zhaocai.business.report.vo.ManagePageReportVo;
import com.zhaocai.business.report.vo.PriceAnalysisReportVo;
import com.zhaocai.business.report.vo.TenderingRateReportVo;
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
