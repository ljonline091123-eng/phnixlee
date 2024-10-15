package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.ContractBaseReportVo;

import java.util.List;

/**
 * 支出合同-合同清单
 */
public interface IContractBaseService {

    List<ContractBaseReportVo> contractLedgerReport(ContractBaseReportVo contractBaseReportVo, String type);

    List<Object> contractLedgerDetails(String contractId);

    List<ContractBaseReportVo> contractLedgerExport(ContractBaseReportVo contractBaseReportVo);

    String getExportTitle(ContractBaseReportVo contractBaseReportVo);
}
