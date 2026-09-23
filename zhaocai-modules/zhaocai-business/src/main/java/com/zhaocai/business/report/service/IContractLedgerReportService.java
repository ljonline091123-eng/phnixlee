package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.ContractLedgerReportVo;

import java.util.List;

/**
 * 合同台账报表Service接口
 */
public interface IContractLedgerReportService {
    List<ContractLedgerReportVo> contractLedgerReport(ContractLedgerReportVo contractLedger);
}
