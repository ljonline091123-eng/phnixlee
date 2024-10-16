package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.ContractListVo;

import java.util.List;

/**
 * 合同-购买材料合同清单
 */
public interface IContractListMaterialsService {

    List<ContractListVo> getDetailsByContractId(String contractId);
}
