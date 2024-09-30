package com.zhaocai.business.agreement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.agreement.domain.AgreementDeposit;
import org.apache.ibatis.annotations.Param;

/**
 * 合同保证金Mapper接口
 *
 * @author chenming
 * @date 2024-05-24
 */
public interface AgreementDepositMapper extends BaseMapper<AgreementDeposit> {

    /**
     * 根据合同删除合同保证金
     *
     * @param agreementId
     */
    void deleteByAgreementId(@Param("agreementId") Long agreementId);
}
