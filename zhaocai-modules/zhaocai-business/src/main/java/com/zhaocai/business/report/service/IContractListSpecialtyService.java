package com.zhaocai.business.report.service;

import java.util.List;

/**
 * 合同-专业分包合同清单
 */
public interface IContractListSpecialtyService {

    List<Object> getDetailsByContractId(String contractId);
}
