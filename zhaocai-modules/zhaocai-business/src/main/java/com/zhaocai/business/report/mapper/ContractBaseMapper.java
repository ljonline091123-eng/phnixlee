package com.zhaocai.business.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.report.domain.ContractBase;
import com.zhaocai.business.report.vo.ContractBaseReportVo;

import java.util.List;

/**
 * 合同基础信息(支出合同)
 */
public interface ContractBaseMapper extends BaseMapper<ContractBase> {

    List<ContractBaseReportVo> contractLedgerList(ContractBaseReportVo contractBaseReportVo);

    List<ContractBaseReportVo> contractLedgerExportList(ContractBaseReportVo contractBaseReportVo);
}
