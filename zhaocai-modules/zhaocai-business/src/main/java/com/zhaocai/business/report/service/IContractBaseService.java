package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.ContractBaseReportVo;
import com.zhaocai.business.report.vo.ContractListVo;

import java.util.List;

/**
 * 支出合同-合同清单
 */
public interface IContractBaseService {

    List<ContractBaseReportVo> contractLedgerReport(ContractBaseReportVo contractBaseReportVo, String type);

    List<ContractListVo> contractLedgerDetails(String contractId);

    List<ContractBaseReportVo> contractLedgerExport(ContractBaseReportVo contractBaseReportVo);

    String getExportTitle(ContractBaseReportVo contractBaseReportVo);
}
