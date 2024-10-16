package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.ContractListVo;

import java.util.List;

/**
 * 合同-其他合同清单
 */
public interface IContractListOtherService {

    List<ContractListVo> getDetailsByContractId(String contractId);
}
