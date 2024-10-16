package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.ContractListVo;

import java.util.List;

/**
 * 合同-租赁设备合同清单
 */
public interface IContractListLeasedDeviceService {

    List<ContractListVo> getDetailsByContractId(String contractId);
}
