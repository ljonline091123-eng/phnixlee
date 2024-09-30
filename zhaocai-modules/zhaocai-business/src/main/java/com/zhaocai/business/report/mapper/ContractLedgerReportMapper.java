package com.zhaocai.business.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.report.vo.ContractLedgerReportVo;

import java.util.List;

public interface ContractLedgerReportMapper extends BaseMapper<ContractLedgerReportVo> {
    List<ContractLedgerReportVo> getContractLedgerReportResult(ContractLedgerReportVo contractLedger);
}
