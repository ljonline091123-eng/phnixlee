package com.zhaocai.business.agreement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.agreement.domain.AgreementMaterialSupply;
import org.apache.ibatis.annotations.Param;

/**
 * 合同-甲供材料清单Mapper接口
 *
 * @author chenming
 * @date 2024-06-26
 */
public interface AgreementMaterialSupplyMapper extends BaseMapper<AgreementMaterialSupply> {

    /**
     * 根据合同-甲供材料清单
     * @param agreementId
     */
    void deleteByAgreementId(@Param("agreementId") Long agreementId);
}
