package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.TenderingRateReportVo;

import java.util.List;

/**
 * 招标率报表Service接口
 */
public interface ITenderingRateReportService {
    List<TenderingRateReportVo> tenderingRateReport(TenderingRateReportVo tenderingRate);
}
