package com.zhaocai.business.report.service;

import java.util.List;

/**
 * 合同-购买材料合同清单
 */
public interface IContractListMaterialsService {

    List<Object> getDetailsByContractId(String contractId);
}
