package com.zhaocai.business.procurement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhaocai.business.procurement.domain.ContractPlanningPushRecord;
import com.zhaocai.business.procurement.mapper.ContractPlanningPushRecordMapper;
import com.zhaocai.business.procurement.service.IContractPlanningPushRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 第三方数据合约规划推送记录 服务实现类
 * </p>
 *
 * @author susiyuan
 * @since 2024-09-12
 */
@Service
public class ContractPlanningPushRecordServiceImpl extends ServiceImpl<ContractPlanningPushRecordMapper, ContractPlanningPushRecord> implements IContractPlanningPushRecordService {

    @Override
    public List<ContractPlanningPushRecord> getByCondition(List<String> contractPlanningIds) {
        if (CollectionUtil.isEmpty(contractPlanningIds)){
            return new ArrayList<>();
        }
        return this.list(new LambdaQueryWrapper<ContractPlanningPushRecord>()
                .in(ContractPlanningPushRecord::getContractPlanningId, contractPlanningIds));
    }

}
