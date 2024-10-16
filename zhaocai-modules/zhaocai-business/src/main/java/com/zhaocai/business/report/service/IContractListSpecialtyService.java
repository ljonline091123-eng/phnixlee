package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.ContractListVo;

import java.util.List;

/**
 * 合同-专业分包合同清单
 */
public interface IContractListSpecialtyService {

    List<ContractListVo> getDetailsByContractId(String contractId);
}
