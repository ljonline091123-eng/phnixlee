package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.req.VendorReportQueryVo;

import java.util.Map;

/**
 * 供应商报表Service接口
 */
public interface IVendorReportService {

    Map<String, Object> getVendorReport(VendorReportQueryVo vo);

    Boolean handleVendorReport();

    Map<String, Object> vendorReportExport(VendorReportQueryVo vo);
}
