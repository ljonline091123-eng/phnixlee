package com.zhaocai.business.report.service;

import java.util.List;

/**
 * 合同-租赁材料合同清单
 */
public interface IContractListLeasedMaterialsService {

    List<Object> getDetailsByContractId(String contractId);
}
