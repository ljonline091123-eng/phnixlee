package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.VBidCountVo;

import java.util.List;

/**
 * 招标率报表Service接口
 */
public interface IVBidCountService {

    List<VBidCountVo> bidCountReport(VBidCountVo vBidCountVo);

    List<VBidCountVo> bidCountReportLazy(VBidCountVo vBidCountVo);

    List<VBidCountVo> bidCountReportExport(VBidCountVo vBidCountVo);

    List<String> getBidCountProjectCode(String id);

    String getExportTitle(VBidCountVo vBidCountVo);

    List<VBidCountVo> getBidCountReport(VBidCountVo vBidCountVo);
}
