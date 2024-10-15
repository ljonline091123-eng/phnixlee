package com.zhaocai.business.report.service;

import java.util.List;

/**
 * 合同-劳务合同清单
 */
public interface IContractListLaborService {

    List<Object> getDetailsByContractId(String contractId);
}
