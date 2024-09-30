package com.zhaocai.business.procurement.service;

import com.zhaocai.business.procurement.domain.ContractPlanningPushRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 第三方数据合约规划推送记录 服务类
 * </p>
 *
 * @author susiyuan
 * @since 2024-09-12
 */
public interface IContractPlanningPushRecordService extends IService<ContractPlanningPushRecord> {

    /**
     * 通过获取记录信息
     * @param contractPlanningIds
     * @return
     * */
    List<ContractPlanningPushRecord> getByCondition(List<String> contractPlanningIds);

}
