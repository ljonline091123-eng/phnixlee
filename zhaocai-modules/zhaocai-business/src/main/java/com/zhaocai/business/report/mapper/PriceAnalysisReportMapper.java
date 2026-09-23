package com.zhaocai.business.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.report.vo.PriceAnalysisReportVo;

import java.util.List;

public interface PriceAnalysisReportMapper extends BaseMapper<PriceAnalysisReportVo> {
    List<PriceAnalysisReportVo> getPriceAnalysisReportResult(PriceAnalysisReportVo priceAnalysis);
}
