package com.zhaocai.business.report.service;

import java.util.List;

/**
 * 合同-其他合同清单
 */
public interface IContractListOtherService {

    List<Object> getDetailsByContractId(String contractId);
}
