package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.*;
import com.zhaocai.business.report.vo.*;
import com.zhaocai.business.report.vo.req.VendorReportQueryVo;
import com.zhaocai.business.report.vo.res.VendorReportListVo;
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
import javax.validation.Valid;
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

    @Autowired
    private IContractBaseService contractBaseService;

    @Autowired
    private IPriceAnalysisByConReportService priceAnalysisByConReportService;

    @Autowired
    private IBidReportService bidReportService;

    @Autowired
    private IVendorReportService vendorReportService;

    @Autowired
    private IProblemReportService problemReportService;

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
//    @GetMapping("/bidCountReport")
//    public ResultData<List<VBidCountVo>> bidCountReport(VBidCountVo vBidCountVo){
//        return ResultData.data(bidCountService.bidCountReport(vBidCountVo));
//    }

    /**
     * 招标率报表(左树右表形式-初始化)
     * @param vBidCountVo
     * @return
     */
    @GetMapping("/bidCountReport")
    public AjaxResult bidCountReport(VBidCountVo vBidCountVo){
        return AjaxResult.success(bidReportService.getInitialInfo(vBidCountVo));
    }

    /**
     * 招标率报表(左树右表形式-获取下一层)
     * @param vBidCountVo
     * @return
     */
    @GetMapping("/getBidCountNext")
    public AjaxResult getBidCountNext(VBidCountVo vBidCountVo){
        return AjaxResult.success(bidReportService.getBidCountNext(vBidCountVo));
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
        Map<String, Object> map = bidReportService.bidReportExport(vBidCountVo);
        ExcelUtil<VBidCountVo> util = new ExcelUtil<VBidCountVo>(VBidCountVo.class);
        util.exportExcel(response, (List<VBidCountVo>) map.get("list"), "招标率报表数据",map.get("title") + "招标率统计报表");
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
     * 合同台账报表（来源于-支出合同）
     * @param contractBaseReportVo
     * @return
     */
    @GetMapping("/contractLedgerByConBase")
    public ResultData<List<ContractBaseReportVo>> contractLedger(ContractBaseReportVo contractBaseReportVo){
        return ResultData.data(contractBaseService.contractLedgerReport(contractBaseReportVo, ""));
    }

    /**
     * 合同台账报表-详情（来源于-支出合同）
     * @param id
     * @return
     */
    @GetMapping("/contractLedgerDetails")
    public AjaxResult contractLedgerDetails(String id){
        List<ContractListVo> list = contractBaseService.contractLedgerDetails(id);
        return AjaxResult.success(list);
    }

    /**
     * 合同台账报表导出（来源于-支出合同）
     * @param contractBaseReportVo
     * @return
     */
    @PostMapping("/contractLedgerExport")
    public void contractLedgerExport(HttpServletResponse response, ContractBaseReportVo contractBaseReportVo){
        List<ContractBaseReportVo> list = contractBaseService.contractLedgerExport(contractBaseReportVo);
        String title = contractBaseService.getExportTitle(contractBaseReportVo);
        ExcelUtil<ContractBaseReportVo> util = new ExcelUtil<ContractBaseReportVo>(ContractBaseReportVo.class);
        util.exportExcel(response,list, "合同台账报表数据",title + "合同台账统计报表");
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
     * 价格分析报表（来源于-支出合同）
     * @param priceAnalysis
     * @return
     */
    @GetMapping("/priceAnalysisReportByCon")
    public ResultData<List<PriceAnalysisByConReportVo>> priceAnalysisReport(PriceAnalysisByConReportVo priceAnalysis){
        return ResultData.data(priceAnalysisByConReportService.priceAnalysisReport(priceAnalysis));
    }

    /**
     * 价格分析报表导出（来源于-支出合同）
     * @param priceAnalysisByConReportVo
     * @return
     */
    @PostMapping("/priceAnalysisExport")
    public void priceAnalysisExport(HttpServletResponse response, PriceAnalysisByConReportVo priceAnalysisByConReportVo){
        List<PriceAnalysisByConReportVo> list = priceAnalysisByConReportService.priceAnalysisExport(priceAnalysisByConReportVo);
        Map<String, Object> title = priceAnalysisByConReportService.getExportTitle(priceAnalysisByConReportVo);
        ExcelUtil<PriceAnalysisByConReportVo> util = new ExcelUtil<PriceAnalysisByConReportVo>(PriceAnalysisByConReportVo.class);
        List<String> columns = (List<String>) title.get("column");
        util.hideColumn(columns.toArray(new String[0]));
        util.exportExcel(response,list, "合同台账报表数据",title.get("title") + "合同台账统计报表");
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

    /**
     * 获取当前组织及以下的组织机构
     * @param id
     * @return
     */
    @GetMapping("/getOrgList")
    public AjaxResult getOrgList(String id) {
        return AjaxResult.success(managePageReportService.getOrgList(id));
    }

    /**
     * 供应商报表
     * @param vo
     * @return
     */
    @GetMapping("/vendorReport")
    public AjaxResult vendorReport(@Valid VendorReportQueryVo vo){
        return AjaxResult.success(vendorReportService.getVendorReport(vo));
    }

    /**
     * 供应商报表导出
     * @param vo
     * @return
     */
    @PostMapping("/vendorReportExport")
    public void vendorReportExport(HttpServletResponse response, VendorReportQueryVo vo){
        Map<String, Object> map = vendorReportService.vendorReportExport(vo);
        ExcelUtil<VendorReportListVo> util = new ExcelUtil<VendorReportListVo>(VendorReportListVo.class);
        util.exportExcel(response, (List<VendorReportListVo>) map.get("list"), "供应商报表数据",map.get("title") + "供应商统计报表");
    }

    /**
     * 问题报表-异常报表-初始化
     * @param queryVO
     * @return
     */
    @GetMapping("/problemReport")
    public AjaxResult problemReport(@Valid ProblemReportVo queryVO) {
        return AjaxResult.success(problemReportService.getInitialInfo(queryVO));
    }

    /**
     * 问题报表-异常报表-下一层数据
     * @param queryVO
     * @return
     */
    @GetMapping("/getProblemNext")
    public AjaxResult getProblemNext(ProblemReportVo queryVO){
        return AjaxResult.success(problemReportService.getProblemNext(queryVO));
    }

    /**
     * 问题报表-异常报表导出
     * @param vo
     * @return
     */
    @PostMapping("/problemReportExport")
    public void problemReportExport(HttpServletResponse response, ProblemReportVo vo){
        Map<String, Object> map = problemReportService.problemReportExport(vo);
        ExcelUtil<ProblemReportVo> util = new ExcelUtil<ProblemReportVo>(ProblemReportVo.class);
        util.exportExcel(response, (List<ProblemReportVo>) map.get("list"), "问题报表-异常报表数据",map.get("title") + "问题报表-异常统计报表");
    }

    /**
     * 问题报表-供应商评价不合格记录(tab2)
     * @param queryVO
     * @return
     */
    @GetMapping("/evaluationBadReport")
    public AjaxResult evaluationBadReport(@Valid EvaluationBadReportVo queryVO) {
        return AjaxResult.success(problemReportService.getEvaluationBadReport(queryVO));
    }

    /**
     * 问题报表-供应商评价不合格记录导出(tab2)
     * @param vo
     * @return
     */
    @PostMapping("/evaluationBadReportExport")
    public void evaluationBadReportExport(HttpServletResponse response, EvaluationBadReportVo vo){
        Map<String, Object> map = problemReportService.evaluationBadReportExport(vo);
        ExcelUtil<EvaluationBadReportVo> util = new ExcelUtil<EvaluationBadReportVo>(EvaluationBadReportVo.class);
        util.exportExcel(response, (List<EvaluationBadReportVo>) map.get("list"), "问题报表-供应商评价不合格情况报表数据",map.get("title") + "问题报表-供应商评价不合格情况统计报表");
    }
}
