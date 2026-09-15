package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.VBidCountVo;

import java.util.List;
import java.util.Map;

public interface IBidReportService {

    List<VBidCountVo> getInitialInfo(VBidCountVo queryVo);

    List<VBidCountVo> getBidCountNext(VBidCountVo queryVo);

    Boolean handleBidReport();

    Map<String, Object> bidReportExport(VBidCountVo vo);
}
