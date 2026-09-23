package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.PriceAnalysisReportVo;

import java.util.List;

/**
 * 招标率报表Service接口
 */
public interface IPriceAnalysisReportService {
    List<PriceAnalysisReportVo> priceAnalysisReport(PriceAnalysisReportVo priceAnalysis);
}
