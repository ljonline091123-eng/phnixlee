package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.ContractListVo;

import java.util.List;

/**
 * 合同-劳务合同清单
 */
public interface IContractListLaborService {

    List<ContractListVo> getDetailsByContractId(String contractId);
}
