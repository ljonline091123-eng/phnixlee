package com.zhaocai.business.report.service;

import java.util.List;

/**
 * 合同-租赁设备合同清单
 */
public interface IContractListLeasedDeviceService {

    List<Object> getDetailsByContractId(String contractId);
}
