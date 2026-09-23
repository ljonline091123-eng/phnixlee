package com.zhaocai.business.agreement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.agreement.domain.AgreementEquipmentSupply;
import org.apache.ibatis.annotations.Param;

/**
 * 合同-甲供设备清单Mapper接口
 *
 * @author chenming
 * @date 2024-06-26
 */
public interface AgreementEquipmentSupplyMapper extends BaseMapper<AgreementEquipmentSupply> {

    /**
     * 根据合同删除
     *
     * @param agreementId
     */
    void deleteByAgreementId(@Param("agreementId") Long agreementId);
}
