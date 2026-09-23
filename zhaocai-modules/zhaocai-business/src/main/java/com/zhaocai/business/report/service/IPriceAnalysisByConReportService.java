package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.PriceAnalysisByConReportVo;

import java.util.List;
import java.util.Map;

/**
 * 价格分析报表Service接口
 */
public interface IPriceAnalysisByConReportService {
    /**
     * 价格分析报表
     * @param priceAnalysis
     * @return
     */
    List<PriceAnalysisByConReportVo> priceAnalysisReport(PriceAnalysisByConReportVo priceAnalysis);

    /**
     * 价格分析报表导出
     * @param priceAnalysisByConReportVo
     * @return
     */
    List<PriceAnalysisByConReportVo> priceAnalysisExport(PriceAnalysisByConReportVo priceAnalysisByConReportVo);

    /**
     * 获取导出报表表头
     * @param priceAnalysisByConReportVo
     * @return
     */
    Map<String, Object> getExportTitle(PriceAnalysisByConReportVo priceAnalysisByConReportVo);
}
