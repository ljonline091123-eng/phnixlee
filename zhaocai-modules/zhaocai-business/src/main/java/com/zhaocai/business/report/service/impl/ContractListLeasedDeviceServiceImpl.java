package com.zhaocai.business.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.report.domain.ContractListLeasedDevice;
import com.zhaocai.business.report.mapper.ContractListLeasedDeviceMapper;
import com.zhaocai.business.report.service.IContractListLeasedDeviceService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 合同-租赁设备合同清单 Service层
 */
@Service
public class ContractListLeasedDeviceServiceImpl extends ServiceImpl<ContractListLeasedDeviceMapper, ContractListLeasedDevice> implements IContractListLeasedDeviceService {

    @Override
    public List<Object> getDetailsByContractId(String contractId) {
        List<ContractListLeasedDevice> list = super.list(new LambdaQueryWrapper<ContractListLeasedDevice>()
                .eq(ContractListLeasedDevice::getConId, contractId)
                .eq(ContractListLeasedDevice::getValid, 0));
        return Collections.singletonList(list);
    }
}
