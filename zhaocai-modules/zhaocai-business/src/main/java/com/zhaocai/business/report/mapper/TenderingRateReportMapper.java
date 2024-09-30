package com.zhaocai.business.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.report.vo.TenderingRateReportVo;

import java.util.List;

public interface TenderingRateReportMapper extends BaseMapper<TenderingRateReportVo> {
    List<TenderingRateReportVo> getTenderingRateReportResult(TenderingRateReportVo tenderingRate);
}
